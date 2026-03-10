package com.kestrel.booking.support;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

/** Builders for domain objects and properties used across unit tests. */
public final class Fixtures {

    public static final Instant T0 = Instant.parse("2026-09-01T08:00:00Z");

    private Fixtures() {
    }

    public static Carrier carrier(long id, String code, int baseHours, String reliability, String maxKg) {
        Carrier carrier = new Carrier(code, code + " Lines", baseHours,
                new BigDecimal(reliability), new BigDecimal(maxKg));
        ReflectionTestUtils.setField(carrier, "id", id);
        return carrier;
    }

    public static Carrier nsl() {
        return carrier(1L, "NSL", 96, "0.940", "24000");
    }

    public static Carrier pcr() {
        return carrier(3L, "PCR", 168, "0.810", "30000");
    }

    public static Shipment shipment(long id, String reference, Carrier carrier, String origin,
                                    String destination, String weightKg, Instant eta) {
        Shipment shipment = new Shipment(reference, origin, destination, carrier.getId(),
                new BigDecimal(weightKg), eta, T0);
        ReflectionTestUtils.setField(shipment, "id", id);
        return shipment;
    }

    public static BookingProperties properties(boolean etaConfidence, double rps, double burstSeconds,
                                               Map<String, Double> overrides) {
        return new BookingProperties(
                new BookingProperties.Lock(true),
                new BookingProperties.Features(etaConfidence),
                new BookingProperties.RateLimit(rps, burstSeconds, overrides),
                new BookingProperties.Eta(0.05, 0.99));
    }

    public static BookingProperties properties() {
        return properties(true, 20, 2, Map.of());
    }
}
