package com.backwell.api_service.modules.products.meilisearch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockLevel {
    NONE("Sin Stock Disponible"),
    LOW("Menos de 10 Disponibles"),
    MEDIUM("Más de 10 Disponibles"),
    HIGH("Más de 20 Disponibles");

    private final String label;
}
