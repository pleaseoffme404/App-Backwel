package com.backwell.api_service.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemPricingDTO(
        UUID itemId,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        BigDecimal discountDecimal
) {
}
