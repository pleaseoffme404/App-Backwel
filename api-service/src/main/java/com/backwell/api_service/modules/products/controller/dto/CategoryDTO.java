package com.backwell.api_service.modules.products.controller.dto;

import com.backwell.api_service.modules.products.jpa.entity.prod.Category;

import java.util.UUID;

public record CategoryDTO(UUID id, String name) {
    public static CategoryDTO fromEntity(Category category) {
        return new CategoryDTO(category.getId(), category.getName());
    }
}
