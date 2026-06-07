package com.backwell.api_service.modules.products.controller.res;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record ProductDTO(
        UUID productId,

        UUID categoryId,
        String categoryName,
        CategoryStepDTO[] path,

        String brand,
        String productName,
        String description,

        Map<UUID, String> attributes,

        List<ItemDTO> items,

        Instant createdAt,
        Instant lastUpdated
) { }
