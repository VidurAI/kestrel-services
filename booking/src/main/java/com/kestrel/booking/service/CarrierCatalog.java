package com.kestrel.booking.service;

import com.kestrel.booking.domain.Carrier;
import com.kestrel.booking.repo.CarrierRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Short-lived in-memory view of the carrier table. Carriers change rarely and are read on every
 * booking, so a one-minute snapshot keeps the hot path off the database.
 */
@Component
public class CarrierCatalog {

    static final Duration TTL = Duration.ofSeconds(60);

    private final CarrierRepository carriers;
    private final Clock clock;
    private volatile Snapshot snapshot;

    public CarrierCatalog(CarrierRepository carriers, Clock clock) {
        this.carriers = carriers;
        this.clock = clock;
    }

    public Optional<Carrier> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(current().byCode().get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public Optional<Carrier> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(current().byId().get(id));
    }

    public List<Carrier> all() {
        return current().ordered();
    }

    /** Drops the cached snapshot; the next read reloads from the database. */
    public void invalidate() {
        snapshot = null;
    }

    private Snapshot current() {
        Snapshot local = snapshot;
        long now = clock.millis();
        if (local == null || now - local.loadedAtMillis() > TTL.toMillis()) {
            local = load(now);
            snapshot = local;
        }
        return local;
    }

    private Snapshot load(long now) {
        List<Carrier> ordered = List.copyOf(carriers.findAllByOrderByCodeAsc());
        return new Snapshot(
                now,
                ordered,
                ordered.stream().collect(Collectors.toUnmodifiableMap(Carrier::getCode, Function.identity())),
                ordered.stream().collect(Collectors.toUnmodifiableMap(Carrier::getId, Function.identity())));
    }

    private record Snapshot(long loadedAtMillis, List<Carrier> ordered,
                            Map<String, Carrier> byCode, Map<Long, Carrier> byId) {
    }
}
