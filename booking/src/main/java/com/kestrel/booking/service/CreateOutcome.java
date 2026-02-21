package com.kestrel.booking.service;

import com.kestrel.booking.domain.Shipment;

/**
 * Result of a create call.
 *
 * @param replayed true when an earlier booking with the same client reference was returned
 */
public record CreateOutcome(Shipment shipment, boolean replayed) {
}
