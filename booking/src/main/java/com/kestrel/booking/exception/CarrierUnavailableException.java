package com.kestrel.booking.exception;

/** The carrier exists but is not accepting bookings. */
public class CarrierUnavailableException extends RuntimeException {

    private final String carrierCode;

    public CarrierUnavailableException(String carrierCode) {
        super("carrier " + carrierCode + " is not accepting bookings");
        this.carrierCode = carrierCode;
    }

    public String getCarrierCode() {
        return carrierCode;
    }
}
