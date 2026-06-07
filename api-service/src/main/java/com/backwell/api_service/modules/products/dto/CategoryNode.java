package com.backwell.api_service.modules.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryNode(
        @NotBlank(message = "Category Name can not be empty.")
        @Pattern(
                regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Category name contains forbidden characters.")
        @Size(max = 100, message = "Category name can not be longer than 100 characters.")
        String name,

        @Size(max = 100)
        String parentName
) {
}
