package com.backwell.api_service.modules.products.controller.res;

import com.backwell.api_service.modules.products.dto.CategoryPath;

import java.util.UUID;

public record CategoryStepDTO(
        UUID categoryId,
        String categoryName
) {

    public static CategoryStepDTO[] fromPath(CategoryPath path) {
        UUID[] ids = path.idPath();
        String[] names = path.namePath();

        CategoryStepDTO[] list = new  CategoryStepDTO[ids.length];

        for (int i = 0; i < path.idPath().length; i++) {
            list[i] = new CategoryStepDTO(ids[i], names[i]);
        }

        return list;
    }

}
