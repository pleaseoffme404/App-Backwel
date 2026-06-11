package com.backwell.api_service.modules.discount.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        indexes = {
                @Index(name = "idx_discount_dates", columnList = "start_date, end_date"),
                @Index(name = "idx_discount_active", columnList = "active")
        }
)
@Builder
public class Discount {
    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal decimalValue;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal decimalFactor;

    @Column(nullable = false)
    private boolean stackable;

    // Regla de Negocio, solo se pueden modificar descuentos que se encuentren antes o
    // durante su periodo de vigencia al momento de la transacción
    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant startDate;

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant endDate;

    @Column(nullable = false)
    private boolean active;

    @Builder.Default
    @OneToMany(
            mappedBy = "discount",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DiscountTarget> targets = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant lastUpdate;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @PrePersist
    protected void onSave() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastUpdate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdate = Instant.now();
    }

    public void addTarget(DiscountTarget target) {
        this.targets.add(target);
        target.setDiscount(this);
    }

    public void removeTarget(DiscountTarget target) {
        this.targets.remove(target);
        target.setDiscount(null);
    }
}
