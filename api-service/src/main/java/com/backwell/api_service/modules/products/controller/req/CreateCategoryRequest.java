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


        // todo finish save description implementation in db
        @Size(max = 512, message = "Description cannot exceed 512 characters.")
        @Pattern(
                regexp = "^[^<>]*$",
                message = "Description contains forbidden HTML or script characters.")
        private final String description;


        public Optional<UUID> getParentId() {
                return Optional.ofNullable(parentId);
        }

        public Optional<String> getDescription() {
                return Optional.ofNullable(description);
        }
}
