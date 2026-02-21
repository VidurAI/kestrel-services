package com.kestrel.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** A freight carrier we can book capacity with. Seeded from {@code db/data.sql}. */
@Entity
@Table(name = "carriers")
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "base_transit_hours", nullable = false)
    private int baseTransitHours;

    /** Historical on-time ratio in the range 0..1. */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal reliability;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    protected Carrier() {
    }

    public Carrier(String code, String name, int baseTransitHours, BigDecimal reliability,
                   BigDecimal maxWeightKg) {
        this.code = code;
        this.name = name;
        this.baseTransitHours = baseTransitHours;
        this.reliability = reliability;
        this.maxWeightKg = maxWeightKg;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getBaseTransitHours() {
        return baseTransitHours;
    }

    public BigDecimal getReliability() {
        return reliability;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }
}
