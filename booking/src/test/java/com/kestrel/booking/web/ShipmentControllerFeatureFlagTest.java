package com.kestrel.booking.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.service.BookingService;
import com.kestrel.booking.service.CarrierCatalog;
import com.kestrel.booking.support.Fixtures;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ShipmentController.class,
        properties = "kestrel.booking.features.eta-confidence=false")
@Import(ControllerTestConfig.class)
class ShipmentControllerFeatureFlagTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private CarrierCatalog carrierCatalog;

    @Test
    void etaConfidenceIsOmittedWhenFeatureIsOff() throws Exception {
        Carrier nsl = Fixtures.nsl();
        when(carrierCatalog.findById(nsl.getId())).thenReturn(Optional.of(nsl));
        when(bookingService.get(5L)).thenReturn(Fixtures.shipment(5L, "KB-00000000000A", nsl, "NLRTM",
                "DEHAM", "100", Fixtures.T0.plus(Duration.ofHours(60))));

        mvc.perform(get("/v2/shipments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrier_code").value("NSL"))
                .andExpect(jsonPath("$.eta_confidence").doesNotExist());
    }
}
