package com.kestrel.booking.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body of {@code POST /v2/shipments}. Wire names are snake_case ({@code carrier_code},
 * {@code weight_kg}, {@code client_reference}).
 */
@DistinctEndpoints
public record CreateShipmentRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}[A-Za-z0-9]{3}$", message = "must be a 5-character UN/LOCODE")
        String origin,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}[A-Za-z0-9]{3}$", message = "must be a 5-character UN/LOCODE")
        String destination,

        @NotBlank
        @Size(min = 3, max = 3, message = "must be a 3-letter carrier code")
        String carrierCode,

        @NotNull
        @DecimalMin(value = "0.1", message = "must be at least 0.1 kg")
        @DecimalMax(value = "50000", message = "must not exceed 50000 kg")
        @Digits(integer = 8, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal weightKg,

        @Size(max = 64, message = "must be at most 64 characters")
        String clientReference) {
}
