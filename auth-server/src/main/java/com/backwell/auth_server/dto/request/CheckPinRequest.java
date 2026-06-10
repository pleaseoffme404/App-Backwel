package com.backwell.auth_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckPinRequest(
        @NotBlank(message = "El PIN es obligatorio")
        @Size(min = 6, max = 6, message = "El PIN debe tener exactamente 6 dígitos, pero tiene ${validatedValue != null ? validatedValue.length() : '0'} caracteres")
        @Pattern(
                regexp = "^\\d+$",
                message = "El PIN solo puede contener números, pero contiene caracteres no numéricos"
        )
        String pin
) {
}
