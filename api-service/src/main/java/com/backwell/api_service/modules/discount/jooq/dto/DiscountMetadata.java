package com.backwell.api_service.modules.discount.jooq.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountMetadata(
        UUID discountId,
        String name,
        BigDecimal decimalValue,
        boolean stackable,
        boolean active,
        Instant startDate,
        Instant endDate,
        Instant createdAt,
        Instant updatedAt
) { }
