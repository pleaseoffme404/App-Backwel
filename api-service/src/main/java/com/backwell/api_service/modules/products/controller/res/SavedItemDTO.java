package com.backwell.api_service.modules.products.controller.res;

import com.backwell.api_service.modules.products.meilisearch.dto.StockLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;


@Builder
public record SavedItemDTO(
        UUID itemId,
        String name,
        String mainPicture,
        String sku,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        BigDecimal discountDecimal,
        StockLevel stockLevel
) {

}