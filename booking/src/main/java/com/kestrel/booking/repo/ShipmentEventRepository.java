package com.kestrel.booking.repo;

import com.kestrel.booking.domain.ShipmentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {

    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtAscIdAsc(Long shipmentId);
}
