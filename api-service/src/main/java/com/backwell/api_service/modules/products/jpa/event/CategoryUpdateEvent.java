package com.backwell.api_service.modules.products.jpa.event;

import java.util.UUID;

public record CategoryUpdateEvent(
        UUID categoryId
) {
}
