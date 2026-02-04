package com.kestrel.booking.service;

import com.kestrel.booking.domain.Carrier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Produces the planned delivery time for a new booking from the carrier's baseline transit time,
 * whether the lane crosses a border, and how much handling the load needs.
 */
@Component
public class EtaEstimator {

    static final double DOMESTIC_FACTOR = 0.4;
    static final int MIN_TRANSIT_HOURS = 12;
    static final int BORDER_CROSSING_HOURS = 36;
    static final int HANDLING_HOURS_PER_BLOCK = 2;
    static final int MAX_HANDLING_HOURS = 24;
    static final BigDecimal BLOCK_KG = BigDecimal.valueOf(500);

    public Instant estimate(Carrier carrier, String origin, String destination,
                            BigDecimal weightKg, Instant start) {
        return start.plus(transit(carrier, origin, destination, weightKg));
    }

    public Duration transit(Carrier carrier, String origin, String destination, BigDecimal weightKg) {
        long hours = Locode.isDomestic(origin, destination)
                ? Math.max(MIN_TRANSIT_HOURS, Math.round(carrier.getBaseTransitHours() * DOMESTIC_FACTOR))
                : carrier.getBaseTransitHours() + BORDER_CROSSING_HOURS;
        return Duration.ofHours(hours + handlingHours(weightKg));
    }

    int handlingHours(BigDecimal weightKg) {
        long blocks = weightKg.divide(BLOCK_KG, 0, RoundingMode.CEILING).longValue();
        return (int) Math.min(MAX_HANDLING_HOURS, blocks * HANDLING_HOURS_PER_BLOCK);
    }
}
