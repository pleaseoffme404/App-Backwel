package com.backwell.api_service.modules.discount.controller.res;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record DiscountDTO(
        UUID discountId,
        String name,
        BigDecimal decimalValue, // Un decimal de 1 posición entera (0) y precisión de 4 decimales
        boolean stackable,
        boolean active,
        Instant startDate,
        Instant endDate,
        Instant createdAt
) {}
