package com.backwell.auth_server.dto.request;


import com.backwell.auth_server.validator.StrongPin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetUpPinAuthenticationRequest(

        @NotBlank(message = "El PIN es obligatorio y no puede estar vacío.")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "El PIN debe constar exactamente de 6 dígitos numéricos."
        )
        @StrongPin
        String pin
) {
}