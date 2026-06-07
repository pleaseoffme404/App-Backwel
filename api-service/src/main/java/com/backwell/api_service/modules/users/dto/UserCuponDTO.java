package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.cupon.CuponType;

import java.math.BigDecimal;
import java.util.UUID;

public record UserCuponDTO(
        UUID id,
        String name,
        CuponType type,
        BigDecimal percentage,
        boolean stackable
){ }
