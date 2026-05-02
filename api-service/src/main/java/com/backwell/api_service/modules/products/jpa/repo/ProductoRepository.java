package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.jpa.entity.prod.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Product, UUID> {
    @NonNull
    Optional<Product> findById(@NonNull UUID id);

    @Query("""
SELECT p
FROM Item i
JOIN i.product p
WHERE p.id = :productId
AND i.id = :itemId
""")
    Optional<Product> findTuple(
            @Param("productId") UUID productId,
            @Param("itemId") UUID itemId);


}
