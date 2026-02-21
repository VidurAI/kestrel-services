package com.kestrel.booking.web;

import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.service.BookingService;
import com.kestrel.booking.service.CreateOutcome;
import com.kestrel.booking.web.dto.CreateShipmentRequest;
import com.kestrel.booking.web.dto.ShipmentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v2/shipments", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShipmentController {

    public static final String REPLAY_HEADER = "Idempotent-Replay";

    private final BookingService bookingService;
    private final ShipmentMapper mapper;

    public ShipmentController(BookingService bookingService, ShipmentMapper mapper) {
        this.bookingService = bookingService;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody CreateShipmentRequest request) {
        CreateOutcome outcome = bookingService.create(mapper.toCommand(request));
        Shipment shipment = outcome.shipment();
        ResponseEntity.BodyBuilder response = ResponseEntity
                .created(URI.create("/v2/shipments/" + shipment.getId()));
        if (outcome.replayed()) {
            response.header(REPLAY_HEADER, "true");
        }
        return response.body(mapper.toResponse(shipment));
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable @Positive Long id) {
        return mapper.toResponse(bookingService.get(id));
    }

    @PostMapping("/{id}/reserve")
    public ShipmentResponse reserve(@PathVariable @Positive Long id) {
        return mapper.toResponse(bookingService.reserve(id));
    }
}
