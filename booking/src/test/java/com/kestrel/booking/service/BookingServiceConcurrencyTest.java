package com.kestrel.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kestrel.booking.domain.ShipmentEvent;
import com.kestrel.booking.domain.ShipmentStatus;
import com.kestrel.booking.exception.ReservationConflictException;
import com.kestrel.booking.repo.ShipmentEventRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Not @Transactional: every worker needs its own committed transaction. */
@SpringBootTest
@ActiveProfiles("test")
class BookingServiceConcurrencyTest {

    private static final int WORKERS = 8;

    @Autowired
    private BookingService service;

    @Autowired
    private ShipmentEventRepository eventRepository;

    @Test
    void onlyOneOfManyConcurrentReservationsSucceeds() throws Exception {
        Long id = service.create(new CreateShipmentCommand(
                "SGSIN", "AUMEL", "PCR", new BigDecimal("1200"), "concurrency-1")).shipment().getId();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < WORKERS; i++) {
                Callable<Boolean> task = () -> {
                    start.await();
                    try {
                        service.reserve(id);
                        return true;
                    } catch (ReservationConflictException e) {
                        return false;
                    }
                };
                results.add(pool.submit(task));
            }
            start.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        assertThat(service.get(id).getStatus()).isEqualTo(ShipmentStatus.RESERVED);
        List<ShipmentEvent> events = eventRepository.findByShipmentIdOrderByOccurredAtAscIdAsc(id);
        assertThat(events.stream().filter(e -> e.getEventType() == ShipmentEvent.Type.RESERVED)).hasSize(1);
        assertThat(events.stream().filter(e -> e.getEventType() == ShipmentEvent.Type.RESERVE_REJECTED))
                .hasSize(WORKERS - 1);
    }
}
