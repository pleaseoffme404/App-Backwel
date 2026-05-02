package com.backwell.auth_server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeRoleRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "El Rol no puede ser nulo")
        @Size(min = 3, max = 20)
        String roleName
) {
}
