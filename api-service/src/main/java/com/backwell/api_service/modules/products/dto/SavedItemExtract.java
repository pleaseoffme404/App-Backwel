package com.backwell.api_service.modules.products.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SavedItemExtract(
        UUID itemId,
        String name,
        String mainPicture,
        String sku,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        BigDecimal discountDecimal
) {
}
