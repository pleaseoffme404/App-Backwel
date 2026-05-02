package com.backwell.api_service.modules.products.jpa.entity.cart;


import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@NamedEntityGraph(
        name = "Cart.withItemsAndVariants",
        attributeNodes = {
                @NamedAttributeNode(value = "cartItems", subgraph = "cartItems-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "cartItems-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode(value = "item")
                        }
                )
        }
)
public class Cart {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false, updatable = false)
    private UserInfo userInfo;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CartItem> cartItems = new ArrayList<>();

    private Instant lastUpdate;


    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdate = Instant.now();
    }

    public static Cart initForUser(UUID userId, UserInfo userInfo) {
        return new Cart(userId, userInfo);
    }

    private Cart(UUID id, UserInfo userInfo) {
        this.id = id;
        this.userInfo = userInfo;
        this.cartItems = new ArrayList<>();
    }

    public void addCartItem(CartItem cartItem) {
        this.cartItems.add(cartItem);
        cartItem.setCart(this);
    }

    public void removeCartItem(CartItem cartItem) {
        this.cartItems.remove(cartItem);
        cartItem.setCart(null);
    }

    /**
     * Add or update
     * @param target A previously checked available Item
     * @param delta A natural number,
     * @param stockLimit The min value between the logical and physical stock limits
     * @param uuidService An {@link UUIDService} instance for the record id generation
     * @throws IllegalArgumentException If the {@code delta} is not a natural number
     * @throws com.backwell.api_service.common.exception.BusinessException When the {@code delta} value is greater than the provided {@code stockLimit} value*/
    public void addOrUpdate(Item target, int delta, int stockLimit, UUIDService uuidService){
        if (delta <= 0) throw new IllegalArgumentException("Delta must be greater than zero");

        cartItems.stream()
                .filter(i-> i.getItem().getId().equals(target.getId()))
                .findFirst()
                .ifPresentOrElse(
                        i -> {
                            int total = i.getSavedQuantity() + delta;
                            if (total > stockLimit) throw new BusinessException("Not enough stock", STOCK_CONFLICT.name());
                            i.setSavedQuantity(total);
                        },
                        () -> {
                            if (delta > stockLimit) throw new BusinessException("Not enough stock", STOCK_CONFLICT.name());
                            this.addCartItem(new CartItem(
                                    uuidService.next(),
                                    target,
                                    delta
                            ));
                        }
                );
    }


    /**
     * Update an existing product in the cart
     * @param target An item currently in the cart
     * @param delta A non 0 integer representing the arithmetical change in the target's cart quantity
     * @param stockLimit The min value between the logical and physical stock limits
     * @throws IllegalArgumentException If the {@code delta} value was 0 or the targeted product was not found in this cart
     * @throws BusinessException When an increase quantity operation exceeds the provided stockLimit value
     * @implNote To be called only fro-m a service layer which provides the stockLimit value*/
    public void updateExisting(Item target, int delta, int stockLimit) {
        if (delta == 0) throw new IllegalArgumentException("Delta must not be zero");

        this.cartItems.stream()
                .filter(i -> i.getItem().getId().equals(target.getId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> {
                            int newQuantity = item.getSavedQuantity() + delta;

                            if (newQuantity <= 0) {
                                this.removeCartItem(item);
                                return;
                            }

                            if (delta > 0 && newQuantity > stockLimit) throw new BusinessException("Not enough stock", STOCK_CONFLICT.name());

                            item.setSavedQuantity(newQuantity);
                        },
                        () -> {
                            throw new IllegalArgumentException("Item not found");
                        }
                );
    }
}
