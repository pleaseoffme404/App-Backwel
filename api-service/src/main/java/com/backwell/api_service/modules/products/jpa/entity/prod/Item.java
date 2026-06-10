package com.backwell.api_service.modules.products.jpa.entity.prod;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "item",
        indexes = {
                @Index(name = "idx_item_product", columnList = "product_id"),
                @Index(name = "idx_item_price", columnList = "base_price"),
                @Index(name = "idx_item_vissible", columnList = "visible")
        }
)
@Builder
public class Item {
    @Id
    @Setter(AccessLevel.NONE)
    UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<ItemAttribute> attributes = new HashSet<>();

    @OneToMany(
            mappedBy = "item",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @OrderBy("order ASC")
    @Builder.Default
    private List<ItemPicture> pictures = new ArrayList<>();


    /**
     * When created, the base price is directly persisted without creating a record in the base price change track*/
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int logicalLimit;

    @Column(nullable = false)
    private boolean visible;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }


    public void addPicture(ItemPicture image) {
        pictures.add(image);
        image.setItem(this);
    }

    public void addPictures(List<ItemPicture> pictures) {
        for (ItemPicture image : pictures) {
            addPicture(image);
        }
    }

    public void removePicture (ItemPicture picture) {
        pictures.remove(picture);
        picture.setItem(null);
    }

    public void addItemAttribute(ItemAttribute attribute) {
        attributes.add(attribute);
        attribute.setItem(this);
    }

    public void addItemAttributes(List<ItemAttribute> attributes) {
        for (ItemAttribute attribute : attributes) {
            addItemAttribute(attribute);
        }
    }

    public void removeAttributeVariant (ItemAttribute attribute) {
        attributes.remove(attribute);
        attribute.setItem(null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item that = (Item) obj;
        return Objects.equals(id, that.id);
    }

    public UUID getProductId(){
        return product.getId();
    }
}
