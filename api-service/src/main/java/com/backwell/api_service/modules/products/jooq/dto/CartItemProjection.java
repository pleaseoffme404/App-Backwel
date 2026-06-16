package com.backwell.api_service.modules.products.jooq.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemProjection (
        UUID itemId,
        String sku,
        String name,
        String pictureUrl,
        int savedQuantity,
        int logicalLimit,
        BigDecimal unitPrice,
        BigDecimal discountDecimal
) { }
