package com.kestrel.booking.ratelimit;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.exception.CarrierCapacityExhaustedException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Admission control in front of carrier capacity. Each carrier owns an independent token bucket;
 * rates come from {@code kestrel.booking.ratelimit.per-carrier-rps} with optional per-carrier
 * overrides.
 */
@Component
public class CarrierRateLimiter {

    // WHY: buckets are keyed per carrier on purpose, never one global bucket. See ADR-0007
    // (kestrel-ops/adr/0007-rate-limit-per-carrier.md) and the 2026-08-21 429 storm: a single
    // shared limit let one saturated carrier starve bookings for every other carrier.
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    private final BookingProperties.RateLimit config;
    private final Clock clock;
    private final MeterRegistry meters;

    public CarrierRateLimiter(BookingProperties properties, Clock clock, MeterRegistry meters) {
        this.config = properties.ratelimit();
        this.clock = clock;
        this.meters = meters;
    }

    /**
     * Takes one token for {@code carrierCode}.
     *
     * @throws CarrierCapacityExhaustedException when the carrier's bucket is empty
     */
    public void acquire(String carrierCode) {
        String key = normalize(carrierCode);
        if (config.rpsFor(key) <= 0) {
            return;
        }
        long now = clock.millis();
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> newBucket(k, now));
        if (!bucket.tryAcquire(now)) {
            meters.counter("booking.ratelimit.rejected", "carrier", key).increment();
            throw new CarrierCapacityExhaustedException(
                    key, Duration.ofMillis(bucket.millisUntilNextToken(now)));
        }
    }

    /** Non-throwing variant for callers that want to degrade instead of fail. */
    public boolean tryAcquire(String carrierCode) {
        try {
            acquire(carrierCode);
            return true;
        } catch (CarrierCapacityExhaustedException e) {
            return false;
        }
    }

    /** Whole tokens currently available for the carrier; mainly for diagnostics. */
    public double available(String carrierCode) {
        String key = normalize(carrierCode);
        TokenBucket bucket = buckets.get(key);
        return bucket == null ? config.capacityFor(key) : bucket.availableTokens(clock.millis());
    }

    private TokenBucket newBucket(String key, long now) {
        return new TokenBucket(config.capacityFor(key), config.rpsFor(key), now);
    }

    private static String normalize(String carrierCode) {
        return carrierCode == null ? "" : carrierCode.trim().toUpperCase(Locale.ROOT);
    }
}
