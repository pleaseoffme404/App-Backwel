package com.backwell.api_service.modules.products.jpa.repo.cart;


import com.backwell.api_service.modules.products.dto.SavedItemExtract;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SavedLaterItemRepository extends JpaRepository<SavedLaterItem, UUID> {

    /**
     * Obtener la lista de Ids de Variante en la lista guardados para más tarde de un usuario
     * */
    @Deprecated
    @Query("""
SELECT i.id
FROM SavedLaterItem sli
JOIN sli.item i
JOIN sli.list sl
WHERE sl.userInfo.uuid = :userId""")
    Set<UUID> findListContainedVariantIds (@Param("userId") UUID userId);

    @Query("""
SELECT new com.backwell.api_service.modules.products.dto.SavedItemExtract(
i.id,
p.name,
i.sku,
i.visible
)
FROM SavedLaterItem sli
JOIN sli.list list
JOIN sli.item i
JOIN i.product p
WHERE list.userInfo.uuid = :userId
""")
    List<SavedItemExtract> extractSavedItemsForUserId(@Param("userId") UUID userId);
}
