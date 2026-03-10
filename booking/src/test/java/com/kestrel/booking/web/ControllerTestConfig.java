package com.kestrel.booking.web;

import com.kestrel.booking.config.BookingProperties;
import com.kestrel.booking.service.EtaConfidenceScorer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Pulls the real mapper and scorer into controller slice tests so serialisation is exercised. */
@TestConfiguration
@EnableConfigurationProperties(BookingProperties.class)
@Import({ShipmentMapper.class, EtaConfidenceScorer.class})
public class ControllerTestConfig {
}
