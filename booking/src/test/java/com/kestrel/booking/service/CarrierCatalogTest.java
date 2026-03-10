package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kestrel.booking.repo.CarrierRepository;
import com.kestrel.booking.support.Fixtures;
import com.kestrel.booking.support.MutableClock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CarrierCatalogTest {

    private CarrierRepository repository;
    private MutableClock clock;
    private CarrierCatalog catalog;

    @BeforeEach
    void setUp() {
        repository = mock(CarrierRepository.class);
        when(repository.findAllByOrderByCodeAsc()).thenReturn(List.of(Fixtures.nsl(), Fixtures.pcr()));
        clock = MutableClock.at("2026-09-01T08:00:00Z");
        catalog = new CarrierCatalog(repository, clock);
    }

    @Test
    void looksUpByCodeAndIdCaseInsensitively() {
        assertThat(catalog.findByCode(" nsl ")).get().extracting("code").isEqualTo("NSL");
        assertThat(catalog.findById(3L)).get().extracting("code").isEqualTo("PCR");
        assertThat(catalog.findByCode("ZZZ")).isEmpty();
        assertThat(catalog.findByCode(null)).isEmpty();
        assertThat(catalog.findById(null)).isEmpty();
    }

    @Test
    void servesFromSnapshotUntilTtlExpires() {
        catalog.all();
        catalog.findByCode("NSL");
        verify(repository, times(1)).findAllByOrderByCodeAsc();

        clock.advance(CarrierCatalog.TTL.plus(Duration.ofSeconds(1)));
        catalog.all();

        verify(repository, times(2)).findAllByOrderByCodeAsc();
    }

    @Test
    void invalidateForcesReload() {
        catalog.all();
        catalog.invalidate();
        catalog.all();

        verify(repository, times(2)).findAllByOrderByCodeAsc();
    }
}
