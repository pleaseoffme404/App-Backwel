package com.backwell.api_service.modules.products.controller.req;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateItemRequest(

        @NotNull
        UUID productId,

        // Un mapa con los uuid de los atributos del producto y los valores que toma cada atributo
        // El keyset debe coincidir con el Set<UUID> de los atributos del producto
        // Si ya existe otro item con los mismos valores, te voy a mandar muy alv
        @NotEmpty(message = "Los atributos del item no pueden estar vacíos")
        Map<@NotBlank UUID, @NotBlank String> itemAttributes,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio base de venta debe ser mayor que 0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal baseSalePrice,

        @NotNull(message = "El stock inicial es obligatorio")
        @PositiveOrZero(message = "El stock inicial no puede ser negativo.")
        Integer initialStock,

        @PositiveOrZero(message = "El stock para redundancia no puede ser negativo.")
        int redundancyStock,

        @NotNull(message = "El limitador lógico es obligatorio")
        @Positive(message = "El limitador lógico debe ser mayor que cero")
        Integer logicalLimit,

        @NotEmpty(message = "Se debe incluir al menos una imagen por cada item")
        List<@NotBlank @URL(message = "Cada imagen debe ser una URL válida") String> images
) {
}
