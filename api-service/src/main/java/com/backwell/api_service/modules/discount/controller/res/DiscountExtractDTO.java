package com.backwell.api_service.modules.discount.controller.res;

import com.backwell.api_service.modules.discount.jooq.dto.DiscountMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DiscountExtractDTO (
        UUID discountId,
        String name,
        BigDecimal decimalValue,
        boolean stackable,
        boolean active,
        Instant startDate,
        Instant endDate,
        Instant createdAt,
        Instant updatedAt,
        Map<UUID, String> affectedCategories,
        Map<UUID, String> targetedProducts,
        Map<UUID, String> targetedItems
){
    public static DiscountExtractDTO from(
            DiscountMetadata m,
            Map<UUID, String> affectedCategories,
            Map<UUID, String> targetedProducts,
            Map<UUID, String> targetedItems) {

        return new DiscountExtractDTO(
                m.discountId(),
                m.name(),
                m.decimalValue(),
                m.stackable(),
                m.active(),
                m.startDate(),
                m.endDate(),
                m.createdAt(),
                m.updatedAt(),
                affectedCategories,
                targetedProducts,
                targetedItems
        );
    }
}
