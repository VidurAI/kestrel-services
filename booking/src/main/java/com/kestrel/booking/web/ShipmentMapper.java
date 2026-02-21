package com.kestrel.booking.web;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.service.CarrierCatalog;
import com.kestrel.booking.service.CreateShipmentCommand;
import com.kestrel.booking.service.EtaConfidenceScorer;
import com.kestrel.booking.service.Locode;
import com.kestrel.booking.web.dto.CreateShipmentRequest;
import com.kestrel.booking.web.dto.ShipmentResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Converts between wire DTOs, service commands and entities. */
@Component
public class ShipmentMapper {

    private final CarrierCatalog carriers;
    private final EtaConfidenceScorer scorer;
    private final BookingProperties properties;

    public ShipmentMapper(CarrierCatalog carriers, EtaConfidenceScorer scorer,
                          BookingProperties properties) {
        this.carriers = carriers;
        this.scorer = scorer;
        this.properties = properties;
    }

    public CreateShipmentCommand toCommand(CreateShipmentRequest request) {
        String clientReference = request.clientReference() == null || request.clientReference().isBlank()
                ? null
                : request.clientReference().trim();
        return new CreateShipmentCommand(
                Locode.normalize(request.origin()),
                Locode.normalize(request.destination()),
                request.carrierCode().trim().toUpperCase(Locale.ROOT),
                request.weightKg(),
                clientReference);
    }

    public ShipmentResponse toResponse(Shipment shipment) {
        Optional<Carrier> carrier = carriers.findById(shipment.getCarrierId());
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getReference(),
                shipment.getOrigin(),
                shipment.getDestination(),
                carrier.map(Carrier::getCode).orElse(null),
                shipment.getStatus().name(),
                shipment.getEta(),
                confidence(shipment, carrier));
    }

    private Double confidence(Shipment shipment, Optional<Carrier> carrier) {
        if (!properties.features().etaConfidence() || carrier.isEmpty() || shipment.getEta() == null) {
            return null;
        }
        Duration plannedTransit = Duration.between(shipment.getCreatedAt(), shipment.getEta());
        return scorer.score(carrier.get(), shipment.getOrigin(), shipment.getDestination(),
                shipment.getWeightKg(), plannedTransit, shipment.getStatus());
    }
}
