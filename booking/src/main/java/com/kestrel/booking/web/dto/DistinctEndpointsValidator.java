package com.kestrel.booking.web.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DistinctEndpointsValidator
        implements ConstraintValidator<DistinctEndpoints, CreateShipmentRequest> {

    @Override
    public boolean isValid(CreateShipmentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.origin() == null || request.destination() == null) {
            return true;
        }
        if (!request.origin().trim().equalsIgnoreCase(request.destination().trim())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("destination")
                .addConstraintViolation();
        return false;
    }
}
