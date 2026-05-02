package com.backwell.api_service.modules.products.jpa.event;


import java.util.UUID;


public record ItemSearchEvent(
        UUID itemId,
        Type type
) {
    private enum Type {
        INDEX,
        DELETE,
    }
}
