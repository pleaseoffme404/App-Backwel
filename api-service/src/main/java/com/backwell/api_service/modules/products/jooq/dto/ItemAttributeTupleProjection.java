package com.backwell.api_service.modules.products.jooq.dto;

import java.util.UUID;

public record ItemAttributeTupleProjection(
        String attributeKey,
        String attributeValue
) {
}
