package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.booking.support.Fixtures;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EtaEstimatorTest {

    private final EtaEstimator estimator = new EtaEstimator();

    @Test
    void crossBorderAddsBorderCrossingTime() {
        Duration transit = estimator.transit(Fixtures.nsl(), "NLRTM", "DEHAM", new BigDecimal("400"));

        // 96h baseline + 36h border + 2h handling for one 500kg block
        assertThat(transit).isEqualTo(Duration.ofHours(134));
    }

    @Test
    void domesticUsesFractionOfBaseline() {
        Duration transit = estimator.transit(Fixtures.nsl(), "NLRTM", "NLAMS", new BigDecimal("400"));

        // round(96 * 0.4) = 38, + 2h handling
        assertThat(transit).isEqualTo(Duration.ofHours(40));
    }

    @Test
    void domesticTransitHasAFloor() {
        var fast = Fixtures.carrier(8L, "FST", 10, "0.900", "1000");

        assertThat(estimator.transit(fast, "NLRTM", "NLAMS", new BigDecimal("1")))
                .isEqualTo(Duration.ofHours(EtaEstimator.MIN_TRANSIT_HOURS + 2));
    }

    @Test
    void handlingTimeGrowsPerBlockAndIsCapped() {
        assertThat(estimator.handlingHours(new BigDecimal("500"))).isEqualTo(2);
        assertThat(estimator.handlingHours(new BigDecimal("501"))).isEqualTo(4);
        assertThat(estimator.handlingHours(new BigDecimal("20000"))).isEqualTo(EtaEstimator.MAX_HANDLING_HOURS);
    }

    @Test
    void estimateAddsTransitToStart() {
        var eta = estimator.estimate(Fixtures.pcr(), "CNSHA", "USLAX", new BigDecimal("500"), Fixtures.T0);

        assertThat(eta).isEqualTo(Fixtures.T0.plus(Duration.ofHours(168 + 36 + 2)));
    }
}
