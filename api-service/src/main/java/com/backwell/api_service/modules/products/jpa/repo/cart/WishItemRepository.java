package com.backwell.api_service.modules.products.jpa.repo.cart;

import com.backwell.api_service.modules.products.jpa.entity.cart.WishItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface WishItemRepository extends JpaRepository<WishItem, Long> {

    /**
     * Obtiene un Set de los Item Id contenidos en la lista principal de un usuario
     * */
    @Query("""
SELECT i.id
FROM WishItem wi
JOIN wi.item i
JOIN wi.wishList wl
WHERE wl.id = :listId
""")
    Set<Long> findListContainedItemIds (@Param("listId") Long listId);
}
