package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(
        indexes = {
                @Index(name = "idx_item_creation_track_item_id", columnList = "item_id")
        }
)
public class ItemCreationTrack {
    @Id
    private UUID itemId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    /**A time-based epoch UUID which sets the transaction cycle when this was checked*/
    private UUID checkedByTransaction;

    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public ItemCreationTrack(Item item) {
        this.item = item;
    }
}
