package com.backwell.api_service.modules.discount.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DiscountTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_target_seq")
    @SequenceGenerator(
            name = "discount_target_seq",
            sequenceName = "discount_target_seq"
    )
    @EqualsAndHashCode.Include
    private Long id;

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
        int targetsCount = 0;
        if (categoryId != null) targetsCount++;
        if (productId != null) targetsCount++;
        if (itemId != null) targetsCount++;

        if (targetsCount != 1) {
            throw new IllegalArgumentException("A Discount target must target exactly one type of entity (category, product, or item)");
        }
    }
}