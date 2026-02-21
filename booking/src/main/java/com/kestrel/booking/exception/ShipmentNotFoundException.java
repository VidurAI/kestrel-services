package com.kestrel.booking.exception;

public class ShipmentNotFoundException extends RuntimeException {

    private final Long shipmentId;

    public ShipmentNotFoundException(Long shipmentId) {
        super("shipment " + shipmentId + " not found");
        this.shipmentId = shipmentId;
    }

    public Long getShipmentId() {
        return shipmentId;
    }
}
