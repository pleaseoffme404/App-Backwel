package com.backwell.api_service.modules.inventory.dto;

import com.backwell.api_service.modules.inventory.entity.InventoryTrace;
import com.backwell.api_service.modules.products.meilisearch.dto.StockLevel;

public record RedisInventoryInfo (
        int availableStock,
        int reservedStock,
        int redundancyStock,
        int physicalStock
) {
    public boolean hasAvailableStock () {
        return availableStock > 0;
    }

    public static RedisInventoryInfo of (InventoryTrace t) {
        return new RedisInventoryInfo(
                t.getAvailableBalance(),
                t.getReservedBalance(),
                t.getRedundancyBalance(),
                t.getPhysicalBalance()
        );
    }

    public String stockLevelLabel() {
        return StockLevel.of(availableStock).getLabel();
    }
}
