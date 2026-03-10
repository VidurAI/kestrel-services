package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.support.Fixtures;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EtaConfidenceScorerTest {

    private final EtaConfidenceScorer scorer = new EtaConfidenceScorer(Fixtures.properties());

    private double score(Carrier carrier, String origin, String destination, String kg, long hours,
                         ShipmentStatus status) {
        return scorer.score(carrier, origin, destination, new BigDecimal(kg), Duration.ofHours(hours), status);
    }

    @Test
    void domesticLaneScoresHigherThanCrossBorder() {
        double domestic = score(Fixtures.nsl(), "NLRTM", "NLAMS", "500", 40, ShipmentStatus.CREATED);
        double crossBorder = score(Fixtures.nsl(), "NLRTM", "DEHAM", "500", 40, ShipmentStatus.CREATED);

        assertThat(domestic).isGreaterThan(crossBorder);
    }

    @Test
    void lessReliableCarrierScoresLower() {
        double reliable = score(Fixtures.nsl(), "NLRTM", "DEHAM", "500", 100, ShipmentStatus.CREATED);
        double flaky = score(Fixtures.pcr(), "NLRTM", "DEHAM", "500", 100, ShipmentStatus.CREATED);

        assertThat(reliable).isGreaterThan(flaky);
    }

    @Test
    void exactValueForKnownInputs() {
        // 0.94 reliability - 0.03 cross-border - 100/2400 horizon = 0.868333 -> 0.87
        assertThat(score(Fixtures.nsl(), "NLRTM", "DEHAM", "500", 100, ShipmentStatus.CREATED))
                .isEqualTo(0.87);
    }

    @Test
    void longerHorizonLowersConfidence() {
        double near = score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 48, ShipmentStatus.CREATED);
        double far = score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 400, ShipmentStatus.CREATED);

        assertThat(near).isGreaterThan(far);
    }

    @Test
    void heavyLoadsArePenalised() {
        double light = score(Fixtures.nsl(), "NLRTM", "DEHAM", "100", 72, ShipmentStatus.CREATED);
        double heavy = score(Fixtures.nsl(), "NLRTM", "DEHAM", "5000", 72, ShipmentStatus.CREATED);
        double veryHeavy = score(Fixtures.nsl(), "NLRTM", "DEHAM", "15000", 72, ShipmentStatus.CREATED);

        assertThat(light).isGreaterThan(heavy);
        assertThat(heavy).isGreaterThan(veryHeavy);
    }

    @Test
    void lifecycleProgressRaisesConfidence() {
        double created = score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 200, ShipmentStatus.CREATED);
        double reserved = score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 200, ShipmentStatus.RESERVED);
        double inTransit = score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 200, ShipmentStatus.IN_TRANSIT);

        assertThat(reserved).isGreaterThan(created);
        assertThat(inTransit).isGreaterThan(reserved);
    }

    @Test
    void deliveredShipmentsAreCertain() {
        assertThat(score(Fixtures.pcr(), "CNSHA", "USLAX", "500", 200, ShipmentStatus.DELIVERED)).isEqualTo(1.0);
    }

    @Test
    void resultStaysInsideConfiguredBounds() {
        Carrier perfect = Fixtures.carrier(9L, "TST", 24, "1.000", "100000");
        Carrier hopeless = Fixtures.carrier(10L, "BAD", 24, "0.010", "100000");

        assertThat(score(perfect, "NLRTM", "NLAMS", "10", 12, ShipmentStatus.IN_TRANSIT)).isEqualTo(0.99);
        assertThat(score(hopeless, "NLRTM", "DEHAM", "20000", 5000, ShipmentStatus.CREATED)).isEqualTo(0.05);
    }

    @Test
    void alwaysBetweenZeroAndOne() {
        for (ShipmentStatus status : ShipmentStatus.values()) {
            double value = score(Fixtures.pcr(), "CNSHA", "USLAX", "30000", 900, status);
            assertThat(value).isBetween(0.0, 1.0);
        }
    }
}
