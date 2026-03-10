package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentEvent;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.exception.CarrierUnavailableException;
import com.kestrel.booking.exception.InvalidShipmentException;
import com.kestrel.booking.exception.ReservationConflictException;
import com.kestrel.booking.exception.ShipmentNotFoundException;
import com.kestrel.booking.exception.UnknownCarrierException;
import com.kestrel.booking.repo.CarrierRepository;
import com.kestrel.booking.repo.ShipmentEventRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceTest {

    @Autowired
    private BookingService service;

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private CarrierCatalog catalog;

    @Autowired
    private ShipmentEventRepository eventRepository;

    @AfterEach
    void resetCatalog() {
        catalog.invalidate();
    }

    private static CreateShipmentCommand command(String carrier, String kg, String clientRef) {
        return new CreateShipmentCommand("NLRTM", "DEHAM", carrier, new BigDecimal(kg), clientRef);
    }

    @Test
    void createPersistsShipmentWithEtaAndInitialEvent() {
        CreateOutcome outcome = service.create(command("NSL", "800", null));

        Shipment shipment = outcome.shipment();
        assertThat(outcome.replayed()).isFalse();
        assertThat(shipment.getId()).isNotNull();
        assertThat(shipment.getReference()).startsWith("KB-");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getEta()).isAfter(shipment.getCreatedAt());
        assertThat(eventRepository.findByShipmentIdOrderByOccurredAtAscIdAsc(shipment.getId()))
                .extracting(ShipmentEvent::getEventType)
                .containsExactly(ShipmentEvent.Type.CREATED);
    }

    @Test
    void sameClientReferenceReplaysExistingBooking() {
        Shipment first = service.create(command("NSL", "800", "order-1")).shipment();

        CreateOutcome second = service.create(command("NSL", "800", "order-1"));

        assertThat(second.replayed()).isTrue();
        assertThat(second.shipment().getId()).isEqualTo(first.getId());
    }

    @Test
    void createRejectsUnknownCarrier() {
        assertThatThrownBy(() -> service.create(command("ZZZ", "10", null)))
                .isInstanceOf(UnknownCarrierException.class)
                .hasMessage("unknown carrier code");
    }

    @Test
    void createRejectsOverweightLoad() {
        assertThatThrownBy(() -> service.create(command("BLX", "18000.01", null)))
                .isInstanceOf(InvalidShipmentException.class)
                .hasMessageContaining("BLX maximum of 18000");
    }

    @Test
    void createRejectsInactiveCarrier() {
        Carrier blx = carrierRepository.findByCodeIgnoreCase("BLX").orElseThrow();
        blx.setActive(false);
        carrierRepository.saveAndFlush(blx);
        catalog.invalidate();

        assertThatThrownBy(() -> service.create(command("BLX", "10", null)))
                .isInstanceOf(CarrierUnavailableException.class);
    }

    @Test
    void reserveMovesShipmentToReserved() {
        Long id = service.create(command("PCR", "300", null)).shipment().getId();

        Shipment reserved = service.reserve(id);

        assertThat(reserved.getStatus()).isEqualTo(ShipmentStatus.RESERVED);
        assertThat(service.get(id).getStatus()).isEqualTo(ShipmentStatus.RESERVED);
        List<ShipmentEvent> events = eventRepository.findByShipmentIdOrderByOccurredAtAscIdAsc(id);
        assertThat(events).extracting(ShipmentEvent::getEventType)
                .containsExactly(ShipmentEvent.Type.CREATED, ShipmentEvent.Type.RESERVED);
    }

    @Test
    void reservingTwiceConflictsAndRecordsRejection() {
        Long id = service.create(command("NSL", "300", null)).shipment().getId();
        service.reserve(id);

        assertThatThrownBy(() -> service.reserve(id))
                .isInstanceOfSatisfying(ReservationConflictException.class,
                        ex -> assertThat(ex.getCurrentStatus()).isEqualTo(ShipmentStatus.RESERVED));
        assertThat(eventRepository.findByShipmentIdOrderByOccurredAtAscIdAsc(id))
                .extracting(ShipmentEvent::getEventType)
                .contains(ShipmentEvent.Type.RESERVE_REJECTED);
    }

    @Test
    void reserveAndGetFailForUnknownShipment() {
        assertThatThrownBy(() -> service.reserve(987_654L)).isInstanceOf(ShipmentNotFoundException.class);
        assertThatThrownBy(() -> service.get(987_654L)).isInstanceOf(ShipmentNotFoundException.class);
    }
}
