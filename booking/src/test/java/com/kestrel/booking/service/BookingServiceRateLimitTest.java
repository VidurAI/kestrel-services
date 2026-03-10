package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kestrel.booking.exception.CarrierCapacityExhaustedException;
import com.kestrel.booking.support.MutableClock;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "kestrel.booking.ratelimit.per-carrier-rps=1",
        "kestrel.booking.ratelimit.burst-seconds=1"})
@ActiveProfiles("test")
@Import(BookingServiceRateLimitTest.FixedClockConfig.class)
class BookingServiceRateLimitTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return MutableClock.at("2026-09-01T08:00:00Z");
        }
    }

    @Autowired
    private BookingService service;

    @Autowired
    private Clock clock;

    private static CreateShipmentCommand command(String carrier, String ref) {
        return new CreateShipmentCommand("NLRTM", "DEHAM", carrier, new BigDecimal("50"), ref);
    }

    @Test
    void rejectsBookingsOnceACarrierBucketIsEmptyButOtherCarriersStillWork() {
        service.create(command("NSL", "rl-1"));

        assertThatThrownBy(() -> service.create(command("NSL", "rl-2")))
                .isInstanceOf(CarrierCapacityExhaustedException.class)
                .hasMessage("carrier capacity exhausted");

        assertThat(service.create(command("BLX", "rl-3")).shipment().getId()).isNotNull();

        ((MutableClock) clock).advance(Duration.ofSeconds(2));
        assertThat(service.create(command("NSL", "rl-4")).shipment().getId()).isNotNull();
    }
}
