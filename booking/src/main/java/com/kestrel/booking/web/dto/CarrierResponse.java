package com.kestrel.booking.web.dto;

import java.math.BigDecimal;

/** Wire representation of a carrier plus its current load. */
public record CarrierResponse(
        Long id,
        String code,
        String name,
        boolean active,
        int baseTransitHours,
        BigDecimal maxWeightKg,
        long openShipments) {
}
