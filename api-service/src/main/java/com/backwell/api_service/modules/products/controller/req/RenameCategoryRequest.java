package com.backwell.api_service.modules.products.controller.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RenameCategoryRequest(
        @NotNull
        UUID targetId,

        @NotBlank
        @Pattern(
                regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Category name contains forbidden characters.")
        @Size(min = 1, max = 100)
        String newName
) {
}
