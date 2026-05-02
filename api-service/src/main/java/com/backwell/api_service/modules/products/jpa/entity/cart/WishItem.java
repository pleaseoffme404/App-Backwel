package com.backwell.api_service.modules.products.jpa.entity.cart;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_with_list_product", columnNames = {"wish_list_id","item_id"})
        }
)
public class WishItem {
    @Id
    private UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wish_list_id", nullable = false, updatable = false)
    private WishList wishList;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    Instant lastUpdate;

    public WishItem(UUID id, Item item) {
        this.id = id;
        this.item = item;
    }
}
