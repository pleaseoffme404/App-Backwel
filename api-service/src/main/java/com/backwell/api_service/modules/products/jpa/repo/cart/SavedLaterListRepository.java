package com.backwell.api_service.modules.products.jpa.repo.cart;

import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterList;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterList_;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedLaterListRepository extends JpaRepository<SavedLaterList, UUID> {
    /**
     * Obtiene la lista de guardados para más tarde para un cliente
     * */
    @Query("""
SELECT sl
FROM SavedLaterList sl
WHERE sl.userInfo.uuid = :userId
""")
    @EntityGraph(value = SavedLaterList_.GRAPH_SAVED_LATER_LIST_WITH_ITEMS_AND_VARIANTS, type = EntityGraph.EntityGraphType.LOAD)
    Optional<SavedLaterList> findListForUser(@Param("userId") UUID userId);
}
