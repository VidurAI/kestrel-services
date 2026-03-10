package com.kestrel.booking.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kestrel.booking.exception.CarrierCapacityExhaustedException;
import com.kestrel.booking.support.Fixtures;
import com.kestrel.booking.support.MutableClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CarrierRateLimiterTest {

    private MutableClock clock;
    private SimpleMeterRegistry meters;
    private CarrierRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = MutableClock.at("2026-09-01T08:00:00Z");
        meters = new SimpleMeterRegistry();
        // 2 rps, 1 second of burst => capacity 2; PCR is overridden to 1 rps => capacity 1
        limiter = new CarrierRateLimiter(
                Fixtures.properties(true, 2, 1, Map.of("PCR", 1.0)), clock, meters);
    }

    @Test
    void allowsUpToBucketCapacityThenRejects() {
        limiter.acquire("NSL");
        limiter.acquire("NSL");

        assertThatThrownBy(() -> limiter.acquire("NSL"))
                .isInstanceOf(CarrierCapacityExhaustedException.class)
                .hasMessage("carrier capacity exhausted");
    }

    @Test
    void rejectionReportsCarrierAndRetryDelay() {
        limiter.acquire("NSL");
        limiter.acquire("NSL");

        assertThatThrownBy(() -> limiter.acquire("nsl"))
                .isInstanceOfSatisfying(CarrierCapacityExhaustedException.class, ex -> {
                    assertThat(ex.getCarrierCode()).isEqualTo("NSL");
                    assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofMillis(500));
                });
    }

    @Test
    void refillsOverTime() {
        limiter.acquire("NSL");
        limiter.acquire("NSL");
        assertThat(limiter.tryAcquire("NSL")).isFalse();

        clock.advance(Duration.ofMillis(500));

        assertThat(limiter.tryAcquire("NSL")).isTrue();
        assertThat(limiter.tryAcquire("NSL")).isFalse();
    }

    @Test
    void carriersHaveIndependentBuckets() {
        limiter.acquire("NSL");
        limiter.acquire("NSL");
        assertThat(limiter.tryAcquire("NSL")).isFalse();

        assertThat(limiter.tryAcquire("BLX")).isTrue();
        assertThat(limiter.tryAcquire("BLX")).isTrue();
        assertThat(limiter.tryAcquire("BLX")).isFalse();
    }

    @Test
    void perCarrierOverrideChangesCapacity() {
        assertThat(limiter.tryAcquire("PCR")).isTrue();
        assertThat(limiter.tryAcquire("PCR")).isFalse();
        assertThat(limiter.available("NSL")).isEqualTo(2.0);
    }

    @Test
    void nonPositiveRateDisablesLimiting() {
        CarrierRateLimiter open = new CarrierRateLimiter(
                Fixtures.properties(true, 0, 1, Map.of()), clock, meters);

        for (int i = 0; i < 1_000; i++) {
            open.acquire("NSL");
        }
    }

    @Test
    void countsRejectionsPerCarrier() {
        limiter.acquire("NSL");
        limiter.acquire("NSL");
        limiter.tryAcquire("NSL");
        limiter.tryAcquire("NSL");

        assertThat(meters.counter("booking.ratelimit.rejected", "carrier", "NSL").count()).isEqualTo(2.0);
    }

    @Test
    void bucketNeverRefillsPastCapacity() {
        limiter.acquire("NSL");
        clock.advance(Duration.ofHours(1));

        assertThat(limiter.available("NSL")).isEqualTo(2.0);
    }
}
