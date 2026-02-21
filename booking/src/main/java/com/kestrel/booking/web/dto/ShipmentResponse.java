package com.kestrel.booking.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Wire representation of a shipment. Serialised with the global snake_case naming strategy, e.g.
 * {@code carrier_code} and {@code eta_confidence} (BOOK-412).
 *
 * @param etaConfidence probability in 0..1 that the shipment arrives by {@code eta}; omitted when
 *                      {@code kestrel.booking.features.eta-confidence} is off
 */
public record ShipmentResponse(
        Long id,
        String reference,
        String origin,
        String destination,
        String carrierCode,
        String status,
        Instant eta,
        @JsonInclude(JsonInclude.Include.NON_NULL) Double etaConfidence) {
}
