package com.kestrel.booking.exception;

/** The request is well-formed but violates a business rule. */
public class InvalidShipmentException extends RuntimeException {

    public InvalidShipmentException(String message) {
        super(message);
    }
}
