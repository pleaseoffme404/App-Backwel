package com.backwell.api_service.modules.products.jpa.repo.cart;


import com.backwell.api_service.modules.products.jpa.entity.cart.Cart;
import com.backwell.api_service.modules.products.jpa.entity.cart.Cart_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(value = Cart_.GRAPH_CART_WITH_ITEMS_AND_VARIANTS, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
SELECT c
FROM Cart c
WHERE c.userInfo.uuid = :userId""")
    Optional<Cart> findCartForUserId(@Param("userId") UUID userId);
}
