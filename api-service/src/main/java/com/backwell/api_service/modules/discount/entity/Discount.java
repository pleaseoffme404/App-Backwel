package com.backwell.api_service.modules.discount.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        indexes = {
                @Index(name = "idx_discount_dates", columnList = "start_date, end_date")
        }
)
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_seq")
    @SequenceGenerator(
            name = "discount_seq",
            sequenceName = "discount_seq"
    )
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal decimalValue;

    @Column(nullable = false)
    private boolean stackable;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "discount",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<DiscountTarget> targets = new ArrayList<>();


    private Instant lastUpdate;
    private Instant createdAt;

    @PrePersist
    protected void onSave() {
        this.createdAt = Instant.now();
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
