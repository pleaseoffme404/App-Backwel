package com.backwell.api_service.modules.products.meilisearch.dto;

import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StockLevel {
    NONE("Sin Stock Disponible", 0),
    LOW("Baja disponibilidad (1 - 10)", 10),
    MEDIUM("Disponible (10 - 20)",  20),
    HIGH("Amplia Disponibilidad (Más de 20)", Integer.MAX_VALUE);



    private final String label;
    private final int maxThreshold;


    // fucking stupid and stupidly efficient XD
    public static StockLevel of(int currentStock) {
        if (currentStock <= 0) return NONE;
        if (currentStock <= 10) return LOW;
        if (currentStock <= 20) return MEDIUM;
        return HIGH;
    }

    public static StockLevel of(RedisInventoryInfo info) {
        return of(info.availableStock());
    }
}