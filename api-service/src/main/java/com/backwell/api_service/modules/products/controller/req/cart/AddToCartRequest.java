package com.backwell.api_service.modules.products.controller.req.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull(message = "El ID del artículo es obligatorio")
        UUID itemId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        @Max(value = 500, message = "No puedes agregar más de 500 unidades de un mismo artículo")
        Integer amount
) {
}
