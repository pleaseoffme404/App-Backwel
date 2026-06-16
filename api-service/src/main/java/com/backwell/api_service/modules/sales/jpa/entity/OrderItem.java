package com.backwell.api_service.modules.sales.jpa.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class OrderItem {
    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @Setter
    private OrderDetail order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false,  updatable = false)
    private Item item;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer amount;

    public OrderItem(UUID id, Item item, BigDecimal unitPrice, int amount) {
        this.id = id;
        this.item = item;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }
}
