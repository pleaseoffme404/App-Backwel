package com.backwell.api_service.modules.inventory.repo;


import com.backwell.api_service.modules.inventory.entity.PriceCalculationHistory;
import com.backwell.api_service.modules.inventory.jooq.repo.PriceCalculationHistoryCustomRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Repository
@Validated
public interface PriceCalculationHistoryRepository extends
        JpaRepository<PriceCalculationHistory, Long>,
        PriceCalculationHistoryCustomRepository

{
}
