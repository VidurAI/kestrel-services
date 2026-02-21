package com.kestrel.booking.web;

import com.kestrel.booking.service.CarrierService.CarrierSummary;
import com.kestrel.booking.web.dto.CarrierResponse;
import org.springframework.stereotype.Component;

@Component
public class CarrierMapper {

    public CarrierResponse toResponse(CarrierSummary summary) {
        var carrier = summary.carrier();
        return new CarrierResponse(
                carrier.getId(),
                carrier.getCode(),
                carrier.getName(),
                carrier.isActive(),
                carrier.getBaseTransitHours(),
                carrier.getMaxWeightKg(),
                summary.openShipments());
    }
}
