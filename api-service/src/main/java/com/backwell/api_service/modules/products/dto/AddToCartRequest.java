package com.backwell.api_service.modules.products.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.UUID;

public record AddToCartRequest(
        @NotNull(message = "El ID del artículo es obligatorio")
        @UUID(message = "El formato del ID del artículo no es válido")
        java.util.UUID itemId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = 500, message = "No puedes agregar más de 500 unidades de un mismo artículo")
        Integer amount
) {
}
