package com.kestrel.booking.service;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.exception.UnknownCarrierException;
import com.kestrel.booking.repo.CarrierLoad;
import com.kestrel.booking.repo.ShipmentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side operations over carriers. */
@Service
public class CarrierService {

    private final CarrierCatalog catalog;
    private final ShipmentRepository shipments;

    public CarrierService(CarrierCatalog catalog, ShipmentRepository shipments) {
        this.catalog = catalog;
        this.shipments = shipments;
    }

    /** A carrier together with how many open shipments it currently holds. */
    public record CarrierSummary(Carrier carrier, long openShipments) {
    }

    @Transactional(readOnly = true)
    public List<CarrierSummary> list() {
        Map<Long, Long> load = shipments.loadByCarrier(ShipmentStatus.openStatuses()).stream()
                .collect(Collectors.toMap(CarrierLoad::carrierId, CarrierLoad::openShipments));
        return catalog.all().stream()
                .map(c -> new CarrierSummary(c, load.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CarrierSummary get(String code) {
        Carrier carrier = catalog.findByCode(code).orElseThrow(() -> new UnknownCarrierException(code));
        long open = shipments.countByCarrierIdAndStatusIn(carrier.getId(), ShipmentStatus.openStatuses());
        return new CarrierSummary(carrier, open);
    }
}
