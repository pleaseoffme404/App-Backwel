package com.backwell.api_service.modules.products.dto;

import java.util.Map;

public record CategoryNode(
        String name,
        String parentName
) {
}
