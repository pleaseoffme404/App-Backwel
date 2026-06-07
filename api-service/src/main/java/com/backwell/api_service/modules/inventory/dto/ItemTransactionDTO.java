package com.backwell.api_service.modules.inventory.dto;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;

public record ItemTransactionDTO(
        Item item,
        int physicalDelta,
        int availableDelta,
        int redundancyDelta,
        int reservedDelta
) {
}
