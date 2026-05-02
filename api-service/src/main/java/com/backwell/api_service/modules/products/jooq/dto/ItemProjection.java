package com.backwell.api_service.modules.products.jooq.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ItemProjection {
    private UUID variantId;
    private UUID productId;
    private String sku;
    private String productName;
    private String description;
    private String mainImage;
    private UUID categoryId;
    private String categoryName;

    private BigDecimal basePrice;
    private BigDecimal finalPrice;
    private Instant lastUpdate;

    private String stockStatus;
    private Boolean hasVariants;
    private BigDecimal discountDecimal;
    private Integer rn;
}
