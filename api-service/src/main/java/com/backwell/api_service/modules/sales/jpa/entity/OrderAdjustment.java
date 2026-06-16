package com.backwell.api_service.modules.sales.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
public class OrderAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_adjustment_seq")
    @SequenceGenerator(
            name = "order_adjustment_seq",
            sequenceName = "order_adjustment_seq",
            allocationSize = 10
    )
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderDetail order;

    @Column(nullable = false, updatable = false)
    private String label;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    public OrderAdjustment(String label, BigDecimal amount) {
        this.label = label;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

}
