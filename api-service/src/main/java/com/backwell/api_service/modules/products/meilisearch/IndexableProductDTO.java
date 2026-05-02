package com.backwell.api_service.modules.products.meilisearch;

import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FieldNameConstants
public record IndexableProductDTO (
        UUID id,
        UUID productId,
        boolean visible,

        String name,
        String sku,
        String brand,
        String mainPicture,

        BigDecimal basePrice,
        BigDecimal currentPrice,

        boolean hasDiscount,
        BigDecimal discountDecimal,
        String discountType,

        boolean inStock,
        String stockLevel,

        UUID categoryId,
        List<String> categoryHierarchy,
        Map<String, String> attributes,
        Instant lastUpdate
) { }
