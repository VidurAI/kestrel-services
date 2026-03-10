package com.kestrel.booking.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kestrel.booking.exception.UnknownCarrierException;
import com.kestrel.booking.service.CarrierService;
import com.kestrel.booking.service.CarrierService.CarrierSummary;
import com.kestrel.booking.support.Fixtures;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CarrierController.class)
@Import(CarrierMapper.class)
class CarrierControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CarrierService carrierService;

    @Test
    void listsCarriersWithLoad() throws Exception {
        when(carrierService.list()).thenReturn(List.of(
                new CarrierSummary(Fixtures.nsl(), 4),
                new CarrierSummary(Fixtures.pcr(), 0)));

        mvc.perform(get("/v2/carriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("NSL"))
                .andExpect(jsonPath("$[0].base_transit_hours").value(96))
                .andExpect(jsonPath("$[0].open_shipments").value(4))
                .andExpect(jsonPath("$[1].code").value("PCR"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    void getsSingleCarrier() throws Exception {
        when(carrierService.get("NSL")).thenReturn(new CarrierSummary(Fixtures.nsl(), 1));

        mvc.perform(get("/v2/carriers/NSL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NSL Lines"))
                .andExpect(jsonPath("$.max_weight_kg").value(24000));
    }

    @Test
    void unknownCarrierCodeIs422() throws Exception {
        when(carrierService.get("ZZZ")).thenThrow(new UnknownCarrierException("ZZZ"));

        mvc.perform(get("/v2/carriers/ZZZ"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("unknown carrier code"));
    }
}
