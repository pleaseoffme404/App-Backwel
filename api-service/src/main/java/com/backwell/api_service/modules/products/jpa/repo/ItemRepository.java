package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @NonNull
    Optional<Item> findById(@NonNull UUID id);

    @Query("""
SELECT i
FROM Item i
WHERE i.id = :itemId
AND i.visible""")
    Optional<Item> getVisibleItem(@Param("itemId") UUID itemId);

    @Query("""
SELECT COUNT(i.id)
FROM Item i
WHERE i.id IN :ids
AND i.visible""")
    long countExistingIds(@Param("ids") Set<UUID> ids);

    default Item getVisibleItemOrThrow(UUID itemId) {
        return getVisibleItem(itemId)
                .orElseThrow(() -> {
                    String msg = String.format("Item with Id `%s` was not fount or is currently unavailable", itemId);
                    return new BusinessException(msg, ITEM_NOT_FOUND.name());
                });
    }
}

