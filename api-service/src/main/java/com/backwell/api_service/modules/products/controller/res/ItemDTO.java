package com.backwell.api_service.modules.products.controller.res;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record ItemDTO (
        UUID itemId,
        String sku,
        boolean visible,
        Map<UUID, String> itemAttributes,
        List<String> pictures,
        BigDecimal basePrice,

        BigDecimal lastCheckedPrice,
        BigDecimal lastCheckedDiscountPercentage,

        UUID lastCheckTransaction,

        Integer availableStock,
        Integer reservedStock,
        Integer redundancyStock,

        Integer physicalStock,

        Instant createdAt,
        Instant lastUpdated
){ }
