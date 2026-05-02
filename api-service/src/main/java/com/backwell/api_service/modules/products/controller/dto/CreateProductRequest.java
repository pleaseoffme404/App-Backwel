package com.backwell.api_service.modules.products.controller.dto;

import com.backwell.api_service.modules.products.controller.validator.ValidProductContract;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.*;

@ValidProductContract
public record CreateProductRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]+$",
                message = "El nombre contiene caracteres no permitidos")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]*$",
                message = "La descripción contiene caracteres no permitidos")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        UUID categoryId,

        @NotBlank(message = "La marca es obligatoria")
        String brand,

        @NotEmpty(message = "Cada producto debe tener al menos un atributo")
        Set<@NotBlank String> attributes,

        @Valid
        @NotEmpty(message = "Cada producto debe tener al menos un Item")
        Set<CreateItemDTO> items
) {}