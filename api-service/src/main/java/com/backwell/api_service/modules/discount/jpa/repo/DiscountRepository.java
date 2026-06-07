package com.backwell.api_service.modules.discount.jpa.repo;

import com.backwell.api_service.modules.discount.jooq.repo.DiscountCustomRepository;
import com.backwell.api_service.modules.discount.jpa.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DiscountRepository extends
        JpaRepository<Discount, UUID>,
        DiscountCustomRepository,
        JpaSpecificationExecutor<Discount>
{
    @Query("""
SELECT d
FROM Discount d
WHERE d.id = :discountId
AND d.endDate > CURRENT_TIMESTAMP""")
    Optional<Discount> getIfUpdatable(@Param("discountId") UUID discountId);



}
