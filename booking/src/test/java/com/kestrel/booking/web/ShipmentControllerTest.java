package com.kestrel.booking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.exception.CarrierCapacityExhaustedException;
import com.kestrel.booking.exception.ReservationConflictException;
import com.kestrel.booking.exception.ShipmentNotFoundException;
import com.kestrel.booking.exception.UnknownCarrierException;
import com.kestrel.booking.service.BookingService;
import com.kestrel.booking.service.CarrierCatalog;
import com.kestrel.booking.service.CreateOutcome;
import com.kestrel.booking.service.CreateShipmentCommand;
import com.kestrel.booking.support.Fixtures;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShipmentController.class)
@Import(ControllerTestConfig.class)
class ShipmentControllerTest {

    private static final String VALID_BODY = """
            {"origin":"nlrtm","destination":"DEHAM","carrier_code":"nsl","weight_kg":1250.5,
             "client_reference":"order-9"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private CarrierCatalog carrierCatalog;

    private Carrier nsl;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        nsl = Fixtures.nsl();
        shipment = Fixtures.shipment(42L, "KB-ABCDEFGH1234", nsl, "NLRTM", "DEHAM", "1250.50",
                Fixtures.T0.plus(Duration.ofHours(50)));
        when(carrierCatalog.findById(nsl.getId())).thenReturn(Optional.of(nsl));
    }

    @Test
    void createReturns201WithSnakeCaseBodyAndEtaConfidence() throws Exception {
        when(bookingService.create(any())).thenReturn(new CreateOutcome(shipment, false));

        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/v2/shipments/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.reference").value("KB-ABCDEFGH1234"))
                .andExpect(jsonPath("$.origin").value("NLRTM"))
                .andExpect(jsonPath("$.destination").value("DEHAM"))
                .andExpect(jsonPath("$.carrier_code").value("NSL"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.eta").exists())
                .andExpect(jsonPath("$.eta_confidence").isNumber());
    }

    @Test
    void createNormalisesRequestIntoCommand() throws Exception {
        when(bookingService.create(any())).thenReturn(new CreateOutcome(shipment, false));

        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateShipmentCommand> captor = ArgumentCaptor.forClass(CreateShipmentCommand.class);
        verify(bookingService).create(captor.capture());
        CreateShipmentCommand command = captor.getValue();
        assertThat(command.origin()).isEqualTo("NLRTM");
        assertThat(command.carrierCode()).isEqualTo("NSL");
        assertThat(command.weightKg()).isEqualByComparingTo(new BigDecimal("1250.5"));
        assertThat(command.clientReference()).isEqualTo("order-9");
    }

    @Test
    void replayedCreateIsFlaggedInHeader() throws Exception {
        when(bookingService.create(any())).thenReturn(new CreateOutcome(shipment, true));

        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string(ShipmentController.REPLAY_HEADER, "true"));
    }

    @Test
    void createRejectsMissingFieldsWithFieldErrorsInSnakeCase() throws Exception {
        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"NLRTM\",\"destination\":\"DEHAM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='carrier_code')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='weight_kg')]").exists());
    }

    @Test
    void createRejectsIdenticalEndpoints() throws Exception {
        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"NLRTM\",\"destination\":\"nlrtm\",\"carrier_code\":\"NSL\","
                                + "\"weight_kg\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destination"))
                .andExpect(jsonPath("$.errors[0].message").value("origin and destination must differ"));
    }

    @Test
    void createRejectsMalformedLocode() throws Exception {
        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"ROTTERDAM\",\"destination\":\"DEHAM\",\"carrier_code\":\"NSL\","
                                + "\"weight_kg\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("origin"));
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCarrierIs422() throws Exception {
        when(bookingService.create(any())).thenThrow(new UnknownCarrierException("ZZZ"));

        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("unknown carrier code"))
                .andExpect(jsonPath("$.carrier_code").value("ZZZ"));
    }

    @Test
    void exhaustedCarrierCapacityIs429WithRetryAfter() throws Exception {
        when(bookingService.create(any()))
                .thenThrow(new CarrierCapacityExhaustedException("NSL", Duration.ofMillis(1500)));

        mvc.perform(post("/v2/shipments").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.error").value("carrier capacity exhausted"))
                .andExpect(jsonPath("$.detail").value("carrier capacity exhausted"))
                .andExpect(jsonPath("$.carrier_code").value("NSL"));
    }

    @Test
    void getReturnsShipment() throws Exception {
        when(bookingService.get(42L)).thenReturn(shipment);

        mvc.perform(get("/v2/shipments/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("KB-ABCDEFGH1234"))
                .andExpect(jsonPath("$.eta_confidence").isNumber());
    }

    @Test
    void getUnknownShipmentIs404Problem() throws Exception {
        when(bookingService.get(7L)).thenThrow(new ShipmentNotFoundException(7L));

        mvc.perform(get("/v2/shipments/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Shipment not found"))
                .andExpect(jsonPath("$.type").value("https://errors.kestrel.example/booking/shipment-not-found"));
    }

    @Test
    void getWithNonPositiveIdIs400() throws Exception {
        mvc.perform(get("/v2/shipments/0")).andExpect(status().isBadRequest());
    }

    @Test
    void reserveReturnsReservedShipment() throws Exception {
        ReflectionTestUtils.setField(shipment, "status", ShipmentStatus.RESERVED);
        when(bookingService.reserve(eq(42L))).thenReturn(shipment);

        mvc.perform(post("/v2/shipments/42/reserve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void reserveConflictIs409() throws Exception {
        when(bookingService.reserve(42L))
                .thenThrow(new ReservationConflictException("KB-ABCDEFGH1234", ShipmentStatus.RESERVED));

        mvc.perform(post("/v2/shipments/42/reserve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.current_status").value("RESERVED"));
    }

    @Test
    void requestIdIsEchoedOrGenerated() throws Exception {
        when(bookingService.get(42L)).thenReturn(shipment);

        mvc.perform(get("/v2/shipments/42").header("X-Request-Id", "gw-123"))
                .andExpect(header().string("X-Request-Id", "gw-123"));
        mvc.perform(get("/v2/shipments/42").header("X-Request-Id", "bad value!"))
                .andExpect(header().exists("X-Request-Id"));
    }
}
