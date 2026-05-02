package com.backwell.api_service.modules.discount.repo;

import com.backwell.api_service.modules.discount.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscountRepository extends
        JpaRepository<Discount, UUID>,
        DiscountCustomRepository
{

}
