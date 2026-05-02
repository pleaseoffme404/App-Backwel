package com.backwell.api_service.modules.products.dto;

import java.util.UUID;

public record SavedItemExtract(
        UUID itemId,
        String name,
        String sku,
        boolean visible
) {
}
