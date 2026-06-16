package com.backwell.api_service.modules.products.jpa.repo.cart;

import com.backwell.api_service.modules.products.dto.CartItemExtract;
import com.backwell.api_service.modules.products.jooq.repo.CartItemCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.cart.CartItem;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends
        JpaRepository<CartItem, UUID>,
        CartItemCustomRepository {

    @Modifying
    @Query("""
DELETE FROM CartItem
WHERE cart.id = :cartId""")
    void clearCart(@Param("cartId") UUID cartId);

    @Query("""
SELECT i
FROM CartItem ci
JOIN ci.item i
JOIN ci.cart c
WHERE c.userInfo.uuid = :userId
""")
    List<Item> getCartItems(@Param("userId") UUID userId);

}
