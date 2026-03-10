package com.kestrel.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kestrel.booking.support.Fixtures;
import org.junit.jupiter.api.Test;

class ShipmentStatusTest {

    @Test
    void legalTransitions() {
        assertThat(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.RESERVED)).isTrue();
        assertThat(ShipmentStatus.RESERVED.canTransitionTo(ShipmentStatus.IN_TRANSIT)).isTrue();
        assertThat(ShipmentStatus.IN_TRANSIT.canTransitionTo(ShipmentStatus.DELIVERED)).isTrue();
        assertThat(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.CANCELLED)).isTrue();
    }

    @Test
    void illegalTransitions() {
        assertThat(ShipmentStatus.RESERVED.canTransitionTo(ShipmentStatus.RESERVED)).isFalse();
        assertThat(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.DELIVERED)).isFalse();
        assertThat(ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.CANCELLED)).isFalse();
        assertThat(ShipmentStatus.CANCELLED.canTransitionTo(ShipmentStatus.RESERVED)).isFalse();
    }

    @Test
    void openStatusesExcludeTerminalOnes() {
        assertThat(ShipmentStatus.openStatuses())
                .containsExactlyInAnyOrder(ShipmentStatus.CREATED, ShipmentStatus.RESERVED, ShipmentStatus.IN_TRANSIT);
        assertThat(ShipmentStatus.DELIVERED.isOpen()).isFalse();
        assertThat(ShipmentStatus.CREATED.isOpen()).isTrue();
    }

    @Test
    void entityRefusesIllegalTransition() {
        Shipment shipment = Fixtures.shipment(1L, "KB-TEST", Fixtures.nsl(), "NLRTM", "DEHAM", "10", Fixtures.T0);
        shipment.transitionTo(ShipmentStatus.RESERVED);

        assertThatThrownBy(() -> shipment.transitionTo(ShipmentStatus.RESERVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESERVED");
    }
}
