package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
@Table(
        indexes = {
                @Index(
                        name = "idx_item_base_price_change_track_item_id",
                        columnList = "item_id"
                )
        }
)
public class ItemBasePriceChangeTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_base_price_change_track_seq")
    @SequenceGenerator(
            name = "item_base_price_change_track_seq",
            sequenceName = "item_base_price_change_track_seq",
            allocationSize = 100
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "item_id", nullable = false,  updatable = false)
    private Item item;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal newPrice;

    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    /**A time based epoch UUID which sets the transaction cycle when this was updated*/
    private UUID checkedByTransaction;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public ItemBasePriceChangeTrack(Item item, BigDecimal newPrice) {
        this.item = item;
        this.newPrice = newPrice;
    }
}
