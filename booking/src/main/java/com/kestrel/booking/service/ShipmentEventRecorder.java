package com.kestrel.booking.service;

import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentEvent;
import com.kestrel.booking.repo.ShipmentEventRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Writes the audit trail and mirrors it to the application log. */
@Component
public class ShipmentEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(ShipmentEventRecorder.class);

    private final ShipmentEventRepository events;
    private final Clock clock;

    public ShipmentEventRecorder(ShipmentEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public void record(Shipment shipment, ShipmentEvent.Type type, String detail) {
        events.save(new ShipmentEvent(shipment.getId(), type, detail, clock.instant()));
        log.info("shipment_event reference={} type={} detail={}", shipment.getReference(), type, detail);
    }
}
