package com.backwell.api_service.modules.products.jpa.entity.prod;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@Table(
        indexes = {
                @Index(name = "idx_product_attribute_porudtc_id", columnList = "product_id")
        }
)
public class ProductAttribute {
    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_key", nullable = false)
    private String key;

    @Override
    public int hashCode() {
        return Objects.hash(id, key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ProductAttribute that = (ProductAttribute) obj;
        return Objects.equals(id, that.id) && Objects.equals(key, that.key);
    }

    public boolean equalsKey(ProductAttribute that) {
        return Objects.equals(key, that.key);
    }
}
