package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Entity
@Table(
        indexes = {
                @Index(name = "idx_price_calculation_history_transaction_id", columnList = "transaction_id")
        }
)
public class PriceCalculationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "price_calculation_history_seq")
    @SequenceGenerator(
            name = "price_calculation_history_seq",
            sequenceName = "price_calculation_history_seq",
            allocationSize = 100
    )
    private Integer id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @Column(precision = 12, scale = 2, nullable = false, updatable = false)
    BigDecimal basePrice;

    @Column(precision = 12, scale = 2, nullable = false,  updatable = false)
    BigDecimal finalPrice;

    @Column(nullable = false, updatable = false, precision = 5, scale = 4)
    private BigDecimal discountDecimal;

    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }


    @Builder
    public PriceCalculationHistory(UUID transactionId, Item item, BigDecimal finalPrice, BigDecimal discountDecimal) {
        this.transactionId = transactionId;
        this.item = item;
        this.finalPrice = finalPrice;
        this.discountDecimal = discountDecimal;
    }
}
