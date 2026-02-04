package com.kestrel.booking.service;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.ShipmentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Scores how likely a shipment is to arrive by its published ETA, as a value between 0 and 1.
 *
 * <p>BOOK-412 exposes this on {@code /v2/shipments} as {@code eta_confidence}; the model itself is
 * described in ADR-0008. It starts from the carrier's historical reliability and adjusts for lane,
 * time horizon, load weight and lifecycle stage.
 */
@Component
public class EtaConfidenceScorer {

    static final double DOMESTIC_BONUS = 0.04;
    static final double CROSS_BORDER_PENALTY = 0.03;
    static final double HORIZON_HOURS_FOR_MAX_PENALTY = 2400.0;
    static final double MAX_HORIZON_PENALTY = 0.25;
    static final double RESERVED_BONUS = 0.03;
    static final double IN_TRANSIT_BONUS = 0.06;
    static final BigDecimal HEAVY_KG = BigDecimal.valueOf(2_000);
    static final BigDecimal VERY_HEAVY_KG = BigDecimal.valueOf(10_000);

    private final BookingProperties.Eta bounds;

    public EtaConfidenceScorer(BookingProperties properties) {
        this.bounds = properties.eta();
    }

    public double score(Carrier carrier, String origin, String destination, BigDecimal weightKg,
                        Duration plannedTransit, ShipmentStatus status) {
        if (status == ShipmentStatus.DELIVERED) {
            return 1.0;
        }
        double score = carrier.getReliability().doubleValue();
        score += Locode.isDomestic(origin, destination) ? DOMESTIC_BONUS : -CROSS_BORDER_PENALTY;
        score -= horizonPenalty(plannedTransit);
        score -= weightPenalty(weightKg);
        score += statusBonus(status);
        return round(clamp(score));
    }

    private static double horizonPenalty(Duration transit) {
        double ratio = transit.toHours() / HORIZON_HOURS_FOR_MAX_PENALTY;
        return Math.min(MAX_HORIZON_PENALTY, Math.max(0.0, ratio));
    }

    private static double weightPenalty(BigDecimal weightKg) {
        if (weightKg.compareTo(VERY_HEAVY_KG) > 0) {
            return 0.05;
        }
        return weightKg.compareTo(HEAVY_KG) > 0 ? 0.02 : 0.0;
    }

    private static double statusBonus(ShipmentStatus status) {
        return switch (status) {
            case RESERVED -> RESERVED_BONUS;
            case IN_TRANSIT -> IN_TRANSIT_BONUS;
            default -> 0.0;
        };
    }

    private double clamp(double value) {
        return Math.max(bounds.confidenceFloor(), Math.min(bounds.confidenceCeiling(), value));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
