package com.backwell.api_service.modules.products.jooq.dto;

import java.util.UUID;

public record CartItemProjection (
        UUID itemId,
        String sku,
        String name,
        String image,
        int savedAmount,
        boolean visible,
        int logicalLimit
) { }
