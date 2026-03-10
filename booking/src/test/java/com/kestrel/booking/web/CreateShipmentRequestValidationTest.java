package com.kestrel.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.booking.web.dto.CreateShipmentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateShipmentRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static Set<String> invalidFields(CreateShipmentRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static CreateShipmentRequest request(String origin, String destination, String carrier, String kg) {
        return new CreateShipmentRequest(origin, destination, carrier,
                kg == null ? null : new BigDecimal(kg), null);
    }

    @Test
    void acceptsValidRequest() {
        assertThat(invalidFields(request("NLRTM", "DEHAM", "NSL", "100.25"))).isEmpty();
    }

    @Test
    void flagsEveryMissingRequiredField() {
        assertThat(invalidFields(request(null, null, null, null)))
                .containsExactlyInAnyOrder("origin", "destination", "carrierCode", "weightKg");
    }

    @Test
    void rejectsMalformedEndpointsAndCarrier() {
        assertThat(invalidFields(request("N1RTM", "DE", "NORTH", "10")))
                .containsExactlyInAnyOrder("origin", "destination", "carrierCode");
    }

    @Test
    void rejectsOutOfRangeWeights() {
        assertThat(invalidFields(request("NLRTM", "DEHAM", "NSL", "0.05"))).containsExactly("weightKg");
        assertThat(invalidFields(request("NLRTM", "DEHAM", "NSL", "50001"))).containsExactly("weightKg");
        assertThat(invalidFields(request("NLRTM", "DEHAM", "NSL", "10.123"))).containsExactly("weightKg");
    }

    @Test
    void rejectsSameOriginAndDestinationIgnoringCase() {
        assertThat(invalidFields(request("NLRTM", "nlrtm", "NSL", "10"))).containsExactly("destination");
    }

    @Test
    void rejectsOverlongClientReference() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                "NLRTM", "DEHAM", "NSL", BigDecimal.TEN, "x".repeat(65));

        assertThat(invalidFields(request)).containsExactly("clientReference");
    }
}
