package com.backwell.api_service.modules.discount.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_discount_target_target_id", columnNames = {"discount_id", "target_type", "target_id"})
        }
)
public class DiscountTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_target_seq")
    @SequenceGenerator(
            name = "discount_target_seq",
            sequenceName = "discount_target_seq"
    )

    @Setter
    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "discount_id",  nullable = false, updatable = false)
    private Discount discount;

    @Column(updatable = false)
    private UUID categoryId;

    @Column(updatable = false)
    private UUID productId;

    @Column(updatable = false)
    private UUID itemId;

    @PrePersist
    protected void onCreate() {
        boolean hasCategory = categoryId != null;
        boolean hasProduct = productId != null;
        boolean hasItem = itemId != null;

        if (!(hasCategory ^ hasProduct ^ hasItem) || hasCategory && hasProduct) {
            throw new IllegalArgumentException("A Discount target must target exactly one type of entity");
        }
    }
}