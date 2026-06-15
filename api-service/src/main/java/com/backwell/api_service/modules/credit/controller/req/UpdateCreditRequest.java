package com.backwell.api_service.modules.credit.controller.req;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCreditRequest(
        @NotNull(message = "El ID de usuario es obligatorio")
        UUID userId,

        @NotNull(message = "La clave de idempotencia es obligatoria")
        UUID idempotencyKey,

        @NotNull(message = "El monto delta es obligatorio")
        @Digits(integer = 4, fraction = 2, message = "El valor debe estar entre -5000.00 y 5000.00")
        @DecimalMin(value = "-5000.00", inclusive = false, message = "El delta debe ser mayor a -5000.00")
        @DecimalMax(value = "5000.00", inclusive = false, message = "El delta debe ser menor a 5000.00")
        BigDecimal delta
) { }