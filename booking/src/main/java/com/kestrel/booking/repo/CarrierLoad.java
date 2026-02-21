package com.kestrel.booking.repo;

/** Number of open shipments currently held by one carrier. */
public record CarrierLoad(Long carrierId, long openShipments) {
}
