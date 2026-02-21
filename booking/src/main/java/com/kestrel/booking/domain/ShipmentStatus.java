package com.kestrel.booking.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Lifecycle of a shipment as seen by booking. */
public enum ShipmentStatus {
    CREATED,
    RESERVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> TRANSITIONS =
            new EnumMap<>(ShipmentStatus.class);

    static {
        TRANSITIONS.put(CREATED, EnumSet.of(RESERVED, CANCELLED));
        TRANSITIONS.put(RESERVED, EnumSet.of(IN_TRANSIT, CANCELLED));
        TRANSITIONS.put(IN_TRANSIT, EnumSet.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
    }

    public boolean canTransitionTo(ShipmentStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** True while the shipment still occupies carrier capacity. */
    public boolean isOpen() {
        return this != DELIVERED && this != CANCELLED;
    }

    public static Set<ShipmentStatus> openStatuses() {
        return EnumSet.of(CREATED, RESERVED, IN_TRANSIT);
    }
}
