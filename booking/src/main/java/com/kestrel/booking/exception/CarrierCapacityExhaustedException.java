package com.kestrel.booking.exception;

import java.time.Duration;

/** Raised when a carrier's token bucket is empty. Surfaces as HTTP 429. */
public class CarrierCapacityExhaustedException extends RuntimeException {

    public static final String MESSAGE = "carrier capacity exhausted";

    private final String carrierCode;
    private final Duration retryAfter;

    public CarrierCapacityExhaustedException(String carrierCode, Duration retryAfter) {
        super(MESSAGE);
        this.carrierCode = carrierCode;
        this.retryAfter = retryAfter;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
