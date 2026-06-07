package com.backwell.api_service.modules.products.event;

import java.util.UUID;

public record UpdateProductCategoryPathEvent(
        UUID categoryId
) {
}
