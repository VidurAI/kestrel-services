package com.kestrel.booking.config;

import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed view of the {@code kestrel.booking.*} configuration tree.
 *
 * <ul>
 *   <li>{@code kestrel.booking.lock.enabled}</li>
 *   <li>{@code kestrel.booking.features.eta-confidence}</li>
 *   <li>{@code kestrel.booking.ratelimit.per-carrier-rps}</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "kestrel.booking")
public record BookingProperties(
        @DefaultValue Lock lock,
        @DefaultValue Features features,
        @DefaultValue RateLimit ratelimit,
        @DefaultValue Eta eta) {

    /** Reservation locking switches. */
    public record Lock(@DefaultValue("true") boolean enabled) {
    }

    /** Feature flags. */
    public record Features(@DefaultValue("true") boolean etaConfidence) {
    }

    /** Bounds applied to every computed ETA confidence value. */
    public record Eta(
            @DefaultValue("0.05") double confidenceFloor,
            @DefaultValue("0.99") double confidenceCeiling) {
    }

    /**
     * Token bucket settings. A non-positive rate disables limiting for that carrier.
     *
     * @param perCarrierRps default sustained requests per second for each carrier
     * @param burstSeconds  bucket capacity expressed as seconds of sustained traffic
     * @param overrides     per-carrier rate overrides keyed by carrier code
     */
    public record RateLimit(
            @DefaultValue("20") double perCarrierRps,
            @DefaultValue("2") double burstSeconds,
            Map<String, Double> overrides) {

        public RateLimit {
            overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        }

        public double rpsFor(String carrierCode) {
            String key = carrierCode == null ? "" : carrierCode.toUpperCase(Locale.ROOT);
            return overrides.getOrDefault(key, perCarrierRps);
        }

        public double capacityFor(String carrierCode) {
            return Math.max(1.0, rpsFor(carrierCode) * burstSeconds);
        }
    }
}
