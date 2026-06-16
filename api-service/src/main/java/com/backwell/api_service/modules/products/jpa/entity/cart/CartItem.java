package com.backwell.api_service.modules.products.jpa.entity.cart;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cart_product", columnNames = {"cart_id", "item_id"})
        }
)
@Getter
public class CartItem {
    @Id
    UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", updatable = false)
    private Cart cart;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false,  updatable = false)
    private Item item;

    @Setter
    private Integer savedQuantity;

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant lastUpdate;

    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdate = Instant.now();
    }

    public CartItem(UUID id, Item item, Integer savedQuantity) {
        this.id = id;
        this.item = item;
        this.savedQuantity = savedQuantity;
    }


    /**
     * @implNote To use only when it's secure you won't get a lazy initialization exception because of an expired hibernate session*/
    public UUID getItemId(){
        return item.getId();
    }
}
