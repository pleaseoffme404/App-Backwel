package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.cupon.CuponType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CuponDTO (
        UUID id,
        String name,
        CuponType type,
        UUID targetId,
        BigDecimal percentage,
        boolean active,
        boolean stackable,
        Instant createdAt,
        Instant usedAt
){ }
