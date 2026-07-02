package com.backwell.auth_server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateRoleRequest(

        @NotNull(message = "El nombre del rol no puede ser nulo")
        @Size(min = 1, max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+(?: [a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+)*$",
                message = "El nombre contiene caracteres no permitidos o espacios duplicados"
        )
        String name,

        @NotNull(message = "El conjunto de permisos no puede ser nulo")
        @NotEmpty(message = "El rol debe contener al menos un permiso")
        Set<@NotNull(message = "El ID del permiso no puede ser nulo") UUID> permissionsSet
) {
}
