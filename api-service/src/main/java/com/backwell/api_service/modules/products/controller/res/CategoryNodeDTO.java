package com.backwell.api_service.modules.products.controller.res;

import com.backwell.api_service.modules.products.dto.CategoryNode;
import com.backwell.api_service.modules.products.jpa.entity.prod.Category;

import java.util.UUID;


public record CategoryNodeDTO (
        UUID categoryId,
        String categoryName,
        UUID parentId
){
    public static CategoryNodeDTO fromEntity(Category c){
        return new CategoryNodeDTO(
                c.getId(),
                c.getName(),
                c.getParent() != null ? c.getParent().getId() : null
        );
    }
}
