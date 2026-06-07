package com.backwell.api_service.modules.products.dto;

import java.util.UUID;

public record CategoryPath(
        UUID[] idPath,
        String[] namePath
) {

}
