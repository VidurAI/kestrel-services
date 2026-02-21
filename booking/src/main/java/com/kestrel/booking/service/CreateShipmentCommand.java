package com.kestrel.booking.service;

import java.math.BigDecimal;

/**
 * Normalised input to {@link BookingService#create(CreateShipmentCommand)}.
 *
 * @param clientReference optional caller-supplied key making the booking idempotent
 */
public record CreateShipmentCommand(
        String origin,
        String destination,
        String carrierCode,
        BigDecimal weightKg,
        String clientReference) {
}
