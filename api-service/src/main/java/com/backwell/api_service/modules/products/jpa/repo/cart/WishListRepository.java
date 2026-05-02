package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.jpa.entity.cart.WishList;
import com.backwell.api_service.modules.products.jpa.entity.cart.WishList_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> { /**
     * Obtiene la lista predeterminada para un cliente
     * @return Una WishList opcional con sus items y variantes pre-cargadas*/
    @Query("""
SELECT wl
FROM WishList wl
JOIN wl.userInfo u
WHERE u.uuid = :userId
AND wl.principalList = TRUE
""")
    @EntityGraph(value = WishList_.GRAPH_WISH_LIST_WITH_ITEMS_AND_VARIANTS, type = EntityGraph.EntityGraphType.LOAD)
    Optional<WishList> findDefaultWishList(@Param("userId") Long userId);
}
