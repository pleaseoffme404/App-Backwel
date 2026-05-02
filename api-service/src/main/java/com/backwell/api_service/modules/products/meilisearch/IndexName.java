package com.backwell.api_service.modules.products.meilisearch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IndexName {
    PRODUCTS("products");

    private final String value;
}
