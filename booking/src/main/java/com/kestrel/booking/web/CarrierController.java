package com.kestrel.booking.web;

import com.kestrel.booking.service.CarrierService;
import com.kestrel.booking.web.dto.CarrierResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v2/carriers", produces = MediaType.APPLICATION_JSON_VALUE)
public class CarrierController {

    private final CarrierService carrierService;
    private final CarrierMapper mapper;

    public CarrierController(CarrierService carrierService, CarrierMapper mapper) {
        this.carrierService = carrierService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CarrierResponse> list() {
        return carrierService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{code}")
    public CarrierResponse get(@PathVariable String code) {
        return mapper.toResponse(carrierService.get(code));
    }
}
