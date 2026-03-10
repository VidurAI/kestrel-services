package com.kestrel.booking.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.support.Fixtures;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository shipments;

    @Autowired
    private CarrierRepository carriers;

    private Long carrierId(String code) {
        return carriers.findByCodeIgnoreCase(code).orElseThrow().getId();
    }

    private Shipment save(String reference, String carrierCode, ShipmentStatus status) {
        Shipment shipment = new Shipment(reference, "NLRTM", "DEHAM", carrierId(carrierCode),
                new BigDecimal("100"), Fixtures.T0.plusSeconds(3600), Fixtures.T0);
        if (status != ShipmentStatus.CREATED) {
            shipment.transitionTo(ShipmentStatus.RESERVED);
        }
        return shipments.saveAndFlush(shipment);
    }

    @Test
    void schemaSeedsTheThreeCarriers() {
        List<Carrier> all = carriers.findAllByOrderByCodeAsc();

        assertThat(all).extracting(Carrier::getCode).containsExactly("BLX", "NSL", "PCR");
        assertThat(all).allMatch(Carrier::isActive);
    }

    @Test
    void findsByReferenceAndProjectsReferenceById() {
        Shipment saved = save("KB-REPO0000001", "NSL", ShipmentStatus.CREATED);

        assertThat(shipments.findByReference("KB-REPO0000001")).get().extracting(Shipment::getId)
                .isEqualTo(saved.getId());
        assertThat(shipments.findReferenceById(saved.getId())).contains("KB-REPO0000001");
        assertThat(shipments.findReferenceById(-1L)).isEmpty();
    }

    @Test
    void referencesAreUnique() {
        save("KB-REPO0000002", "NSL", ShipmentStatus.CREATED);

        assertThatThrownBy(() -> save("KB-REPO0000002", "BLX", ShipmentStatus.CREATED))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aggregatesOpenShipmentsPerCarrier() {
        save("KB-REPO0000003", "PCR", ShipmentStatus.CREATED);
        save("KB-REPO0000004", "PCR", ShipmentStatus.RESERVED);

        assertThat(shipments.loadByCarrier(ShipmentStatus.openStatuses()))
                .contains(new CarrierLoad(carrierId("PCR"), 2));
        assertThat(shipments.countByCarrierIdAndStatusIn(carrierId("PCR"), List.of(ShipmentStatus.RESERVED)))
                .isEqualTo(1);
        assertThat(shipments.findByCarrierIdAndStatus(carrierId("PCR"), ShipmentStatus.RESERVED)).hasSize(1);
    }

    @Test
    void timestampsArePopulatedOnInsert() {
        Shipment saved = save("KB-REPO0000005", "NSL", ShipmentStatus.CREATED);

        assertThat(saved.getCreatedAt()).isEqualTo(Fixtures.T0);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
