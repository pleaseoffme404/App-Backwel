package com.backwell.api_service.modules.products.dto;

import java.util.UUID;

public record CartItemExtract(
        UUID itemId,
        String sku,
        String name,
        int savedQuantity,
        boolean visible,
        int logicalLimit
) { }