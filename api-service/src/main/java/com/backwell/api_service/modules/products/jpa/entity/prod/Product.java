package com.backwell.api_service.modules.products.jpa.entity.prod;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        indexes = {
                @Index(name = "idx_product_name", columnList = "name", unique = true),
                @Index(name = "idx_product_created_at", columnList = "created_at DESC"),
                @Index(name = "idx_product_category", columnList = "category_id"),
        }
)
@NamedEntityGraph(
        name = "Product.fetchPostCreationDetails",
        attributeNodes = {
                @NamedAttributeNode("attributes"),
                @NamedAttributeNode(value = "items", subgraph = "item-details-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "item-details-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("pictures"),
                                @NamedAttributeNode("attributes")
                        }
                )
        }
)
@Builder
public class Product {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    private Set<ProductAttribute> attributes = new HashSet<>();


    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Item> items = new HashSet<>();

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @Column(nullable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant updatedAt;

    @PrePersist
    public void onSave() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public void pushAttributes(Set<ProductAttribute> attributes) {
        this.attributes.addAll(attributes);
        attributes.forEach(attribute -> attribute.setProduct(this));
    }


    public void addAttribute(ProductAttribute attribute) {
        attributes.add(attribute);
        attribute.setProduct(this);
    }

    public void removeAttribute(ProductAttribute attribute) {
        attributes.remove(attribute);
        attribute.setProduct(null);
    }

    public void addItem(Item item) {
        items.add(item);
        item.setProduct(this);
    }
    public void removeItem(Item item) {
        items.remove(item);
        item.setProduct(null);
    }

    public UUID getCategoryId() {
        return category.getId();
    }
}
