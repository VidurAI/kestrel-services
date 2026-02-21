package com.kestrel.booking.exception;

import com.kestrel.booking.domain.ShipmentStatus;

/** The shipment is not in a state that allows it to be reserved. */
public class ReservationConflictException extends RuntimeException {

    private final String reference;
    private final ShipmentStatus currentStatus;

    public ReservationConflictException(String reference, ShipmentStatus currentStatus) {
        super("shipment " + reference + " is " + currentStatus + " and cannot be reserved");
        this.reference = reference;
        this.currentStatus = currentStatus;
    }

    public String getReference() {
        return reference;
    }

    public ShipmentStatus getCurrentStatus() {
        return currentStatus;
    }
}
