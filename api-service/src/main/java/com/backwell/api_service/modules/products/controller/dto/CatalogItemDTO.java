package com.backwell.api_service.modules.products.controller.dto;

import com.backwell.api_service.modules.products.jooq.dto.ItemProjection;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CatalogItemDTO(
        UUID uuid,
        String name,
        String description,
        String mainPicture,
        Map<String, String> attributes,

        UUID categoryId,
        String category,

        BigDecimal basePrice,
        BigDecimal finalPrice,
        BigDecimal discountDecimal,
        String stockStatus,

        boolean hasVariants
) {
}
