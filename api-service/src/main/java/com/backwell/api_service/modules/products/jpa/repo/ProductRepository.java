package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.jpa.entity.prod.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @NonNull
    Optional<Product> findById(@NonNull UUID id);

    @Query("""
SELECT COUNT(p.id)
FROM Product p
WHERE p.id IN :ids""")
    long countExistingIds(@Param("ids") Set<UUID> ids);

    @EntityGraph(value = "Product.fetchPostCreationDetails", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
SELECT p
FROM Product p
WHERE p.id = :id""")
    Optional<Product> fetchPostCreationDetails(@Param("id") UUID id);

}
