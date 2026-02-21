package com.kestrel.booking.service;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentEvent;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.exception.CarrierUnavailableException;
import com.kestrel.booking.exception.InvalidShipmentException;
import com.kestrel.booking.exception.ReservationConflictException;
import com.kestrel.booking.exception.ShipmentNotFoundException;
import com.kestrel.booking.exception.UnknownCarrierException;
import com.kestrel.booking.lock.AdvisoryLock;
import com.kestrel.booking.ratelimit.CarrierRateLimiter;
import com.kestrel.booking.repo.ShipmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Core booking use cases: create, look up and reserve shipments. */
@Service
public class BookingService {

    private final ShipmentRepository shipments;
    private final CarrierCatalog carriers;
    private final CarrierRateLimiter rateLimiter;
    private final AdvisoryLock advisoryLock;
    private final EtaEstimator etaEstimator;
    private final ReferenceGenerator references;
    private final ShipmentEventRecorder events;
    private final Clock clock;

    public BookingService(ShipmentRepository shipments, CarrierCatalog carriers,
                          CarrierRateLimiter rateLimiter, AdvisoryLock advisoryLock,
                          EtaEstimator etaEstimator, ReferenceGenerator references,
                          ShipmentEventRecorder events, Clock clock) {
        this.shipments = shipments;
        this.carriers = carriers;
        this.rateLimiter = rateLimiter;
        this.advisoryLock = advisoryLock;
        this.etaEstimator = etaEstimator;
        this.references = references;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public CreateOutcome create(CreateShipmentCommand command) {
        String reference = references.generate(command.clientReference());
        Optional<Shipment> existing = shipments.findByReference(reference);
        if (existing.isPresent()) {
            return new CreateOutcome(existing.get(), true);
        }

        Carrier carrier = carriers.findByCode(command.carrierCode())
                .orElseThrow(() -> new UnknownCarrierException(command.carrierCode()));
        validate(command, carrier);
        rateLimiter.acquire(carrier.getCode());

        Instant now = clock.instant();
        Instant eta = etaEstimator.estimate(
                carrier, command.origin(), command.destination(), command.weightKg(), now);
        Shipment saved = shipments.save(new Shipment(
                reference, command.origin(), command.destination(), carrier.getId(),
                command.weightKg(), eta, now));
        events.record(saved, ShipmentEvent.Type.CREATED, "carrier=" + carrier.getCode());
        return new CreateOutcome(saved, false);
    }

    @Transactional(readOnly = true)
    public Shipment get(Long id) {
        return shipments.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    @Transactional(noRollbackFor = ReservationConflictException.class)
    public Shipment reserve(Long id) {
        String reference = shipments.findReferenceById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(id));
        advisoryLock.acquire(reference);

        Shipment shipment = shipments.findById(id).orElseThrow(() -> new ShipmentNotFoundException(id));
        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.RESERVED)) {
            events.record(shipment, ShipmentEvent.Type.RESERVE_REJECTED,
                    "status=" + shipment.getStatus());
            throw new ReservationConflictException(reference, shipment.getStatus());
        }

        Carrier carrier = carriers.findById(shipment.getCarrierId())
                .orElseThrow(() -> new UnknownCarrierException(String.valueOf(shipment.getCarrierId())));
        if (!carrier.isActive()) {
            throw new CarrierUnavailableException(carrier.getCode());
        }
        rateLimiter.acquire(carrier.getCode());

        shipment.transitionTo(ShipmentStatus.RESERVED);
        Shipment saved = shipments.save(shipment);
        events.record(saved, ShipmentEvent.Type.RESERVED, "carrier=" + carrier.getCode());
        return saved;
    }

    private static void validate(CreateShipmentCommand command, Carrier carrier) {
        if (!carrier.isActive()) {
            throw new CarrierUnavailableException(carrier.getCode());
        }
        if (command.weightKg().compareTo(carrier.getMaxWeightKg()) > 0) {
            throw new InvalidShipmentException(
                    "weight exceeds the " + carrier.getCode() + " maximum of "
                            + carrier.getMaxWeightKg().stripTrailingZeros().toPlainString() + " kg");
        }
        if (!Locode.isValid(command.origin()) || !Locode.isValid(command.destination())) {
            throw new InvalidShipmentException("origin and destination must be UN/LOCODE values");
        }
        if (command.origin().equals(command.destination())) {
            throw new InvalidShipmentException("origin and destination must differ");
        }
    }
}
