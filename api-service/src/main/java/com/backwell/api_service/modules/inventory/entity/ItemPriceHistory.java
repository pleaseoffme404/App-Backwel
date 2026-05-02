package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ItemPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_price_history_seq")
    @SequenceGenerator(
            name = "item_price_history_seq",
            sequenceName = "item_price_history_seq",
            allocationSize = 5
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "item_id", nullable = false,  updatable = false)
    private Item item;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private BigDecimal nominalPrice;

    private Instant lastUpdate;

    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

    @Builder
    public ItemPriceHistory(Item item, BigDecimal basePrice, BigDecimal nominalPrice) {
        this.item = item;
        this.basePrice = basePrice;
        this.nominalPrice = nominalPrice;
    }
}
