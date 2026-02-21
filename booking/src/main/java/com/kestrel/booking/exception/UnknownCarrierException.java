package com.kestrel.booking.exception;

public class UnknownCarrierException extends RuntimeException {

    public static final String MESSAGE = "unknown carrier code";

    private final String carrierCode;

    public UnknownCarrierException(String carrierCode) {
        super(MESSAGE);
        this.carrierCode = carrierCode;
    }

    public String getCarrierCode() {
        return carrierCode;
    }
}
