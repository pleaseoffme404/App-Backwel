package com.backwell.api_service.modules.products.jpa.entity.cart;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wish_list_user",
                        columnNames = {"user_id", "principal_list"}
                )
        }
)
@NamedEntityGraphs(
        @NamedEntityGraph(
                name = "WishList.withItemsAndVariants", attributeNodes = {
                        @NamedAttributeNode(value = "items", subgraph = "items-subgraph")},
                subgraphs = {
                        @NamedSubgraph(
                                name = "items-subgraph",
                                attributeNodes = {
                                        @NamedAttributeNode(value = "item")
                                }
                        )
                })
)
public class WishList {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,  updatable = false)
    private UserInfo userInfo;

    @Column(unique = true, nullable = false)
    private String tittle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(
            mappedBy = "wishList",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WishItem> items = new ArrayList<>();

    @Column(nullable = false)
    private boolean principalList;

    private Instant lastUpdate;

    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdate = Instant.now();
    }

    public WishList(
            UUID id,
            UserInfo userInfo,
            String tittle,
            String description,
            boolean principalList
            ) {
        this.id = id;
        this.userInfo = userInfo;
        this.tittle = tittle;
        this.description = description;
    }

    public static WishList initForUser(UUID id, UserInfo userInfo) {
        return new WishList(id, userInfo, "Lista Principal", "Lista Principal de Artículos", true);
    }


    /**
     * Adds an item to {@code this} WishList
     * @implNote Called exclusively from the service layer, a call to this method without validation to prevent duplicates in the list items could cause an SQL Exception by breaking the unique constraint when trying to persist changes.*/
    public void addItem(WishItem item) {
        this.items.add(item);
        item.setWishList(this);
    }

    public void removeItem(WishItem item) {
        this.items.remove(item);
        item.setWishList(null);
    }
}
