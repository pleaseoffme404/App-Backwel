package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.modules.products.controller.dto.MessageResponse;

import java.util.UUID;

public interface CategoryCustomRepository {
    boolean hasProducts(UUID categoryId);
    boolean hasChildren(UUID categoryId);
    boolean isDescending(UUID potentialParent, UUID currentCategoryId);

    String[] buildHierarchy(UUID categoryId);

    void checkUniqueNameConstraint(String newName);
}
