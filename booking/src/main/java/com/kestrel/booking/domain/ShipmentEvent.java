package com.kestrel.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only audit record of something that happened to a shipment. */
@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {

    public enum Type {
        CREATED,
        RESERVED,
        RESERVE_REJECTED,
        ETA_REVISED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private Type eventType;

    @Column(length = 255)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ShipmentEvent() {
    }

    public ShipmentEvent(Long shipmentId, Type eventType, String detail, Instant occurredAt) {
        this.shipmentId = shipmentId;
        this.eventType = eventType;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getShipmentId() {
        return shipmentId;
    }

    public Type getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
