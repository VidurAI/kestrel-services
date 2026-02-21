package com.kestrel.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** A booked consignment travelling between two UN/LOCODE endpoints on one carrier. */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 24, updatable = false)
    private String reference;

    @Column(nullable = false, length = 5)
    private String origin;

    @Column(nullable = false, length = 5)
    private String destination;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    private Instant eta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shipment() {
    }

    public Shipment(String reference, String origin, String destination, Long carrierId,
                    BigDecimal weightKg, Instant eta, Instant createdAt) {
        this.reference = reference;
        this.origin = origin;
        this.destination = destination;
        this.carrierId = carrierId;
        this.weightKg = weightKg;
        this.eta = eta;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Moves the shipment to {@code target}.
     *
     * @throws IllegalStateException if the lifecycle does not permit the transition
     */
    public void transitionTo(ShipmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "cannot move shipment " + reference + " from " + status + " to " + target);
        }
        this.status = target;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Long getCarrierId() {
        return carrierId;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public Instant getEta() {
        return eta;
    }

    public void setEta(Instant eta) {
        this.eta = eta;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
