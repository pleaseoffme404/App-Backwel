package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.jpa.entity.prod.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    @Deprecated
    @Query("""
SELECT pa
FROM ProductAttribute pa
JOIN pa.product p
where p.id IN :id
ORDER BY p.id, pa.id
""")
    List<ProductAttribute> findByProductIds(@Param("ids") List<Long> ids);
}
