package com.backwell.api_service.modules.products.meilisearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FieldNameConstants
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IndexableProductDTO (
        UUID id,
        UUID productId,
        boolean visible,

        String name,
        String productDescription,
        String sku,
        String brand,
        String mainPicture,

        UUID transactionId,
        BigDecimal basePrice,
        BigDecimal currentPrice,

        boolean hasDiscount,
        BigDecimal discountPercentage,

        boolean inStock,
        String stockLevel,

        UUID categoryId,
        List<String> categoryHierarchy,
        Map<String, String> attributes,
        Instant lastUpdate
) { }
