package com.backwell.api_service.modules.products.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDTO(
        UUID itemId,
        String sku,
        String name,
        String pictureUrl,
        int amount,
        int stockLimit,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        BigDecimal discountDecimal
) { }
