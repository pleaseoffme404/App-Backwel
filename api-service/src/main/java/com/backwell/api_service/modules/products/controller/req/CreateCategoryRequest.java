package com.backwell.api_service.modules.products.controller.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public class CreateCategoryRequest {
        @NotBlank(message = "Category Name can not be empty")
        @Pattern(
                regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Category name contains forbidden characters.")
        @Size(max = 100)
        @Getter
        private final String categoryName;

        private final UUID parentId;


        public Optional<UUID> getParentId() {
                return Optional.ofNullable(parentId);
        }
}
