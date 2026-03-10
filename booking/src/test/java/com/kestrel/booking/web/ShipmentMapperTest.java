package com.kestrel.booking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.domain.Shipment;
import com.kestrel.booking.service.CarrierCatalog;
import com.kestrel.booking.service.CreateShipmentCommand;
import com.kestrel.booking.service.EtaConfidenceScorer;
import com.kestrel.booking.support.Fixtures;
import com.kestrel.booking.web.dto.CreateShipmentRequest;
import com.kestrel.booking.web.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShipmentMapperTest {

    private CarrierCatalog catalog;
    private Carrier nsl;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        catalog = mock(CarrierCatalog.class);
        nsl = Fixtures.nsl();
        when(catalog.findById(nsl.getId())).thenReturn(Optional.of(nsl));
        shipment = Fixtures.shipment(9L, "KB-MAPPER00001", nsl, "NLRTM", "NLAMS", "100",
                Fixtures.T0.plus(Duration.ofHours(40)));
    }

    private ShipmentMapper mapper(boolean etaConfidence) {
        BookingProperties props = Fixtures.properties(etaConfidence, 20, 2, Map.of());
        return new ShipmentMapper(catalog, new EtaConfidenceScorer(props), props);
    }

    @Test
    void mapsEntityToResponse() {
        ShipmentResponse response = mapper(true).toResponse(shipment);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.carrierCode()).isEqualTo("NSL");
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.eta()).isEqualTo(shipment.getEta());
        assertThat(response.etaConfidence()).isBetween(0.0, 1.0);
    }

    @Test
    void omitsConfidenceWhenFeatureDisabled() {
        assertThat(mapper(false).toResponse(shipment).etaConfidence()).isNull();
    }

    @Test
    void unknownCarrierYieldsNullCodeAndNoConfidence() {
        when(catalog.findById(nsl.getId())).thenReturn(Optional.empty());

        ShipmentResponse response = mapper(true).toResponse(shipment);

        assertThat(response.carrierCode()).isNull();
        assertThat(response.etaConfidence()).isNull();
    }

    @Test
    void normalisesRequestIntoCommand() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                " nlrtm", "deham ", "nsl", new BigDecimal("12.50"), "   ");

        CreateShipmentCommand command = mapper(true).toCommand(request);

        assertThat(command.origin()).isEqualTo("NLRTM");
        assertThat(command.destination()).isEqualTo("DEHAM");
        assertThat(command.carrierCode()).isEqualTo("NSL");
        assertThat(command.clientReference()).isNull();
    }
}
