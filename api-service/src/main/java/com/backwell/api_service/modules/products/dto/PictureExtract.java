package com.backwell.api_service.modules.products.dto;

import java.util.UUID;

public record PictureExtract (
        UUID itemId,
        String url,
        int order
){
}
