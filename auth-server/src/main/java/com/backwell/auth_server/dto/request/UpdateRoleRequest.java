package com.backwell.auth_server.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record UpdateRoleRequest(
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
        @Pattern(
                regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+(\\s+[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ]+)*$",
                message = "El nombre contiene caracteres no permitidos o espacios inválidos."
        )
        String newName,

        @NotEmpty(message = "Si se proporciona el conjunto de permisos, este debe contener al menos un elemento.")
        Set<UUID> newPermissionsSet
) {

    // Constructor compacto para la validación cruzada de campos
    public UpdateRoleRequest {
        if (newName != null) {
            newName = newName.trim();
        }

        if (newName == null && newPermissionsSet == null) {
            throw new IllegalArgumentException("Al menos uno de los campos (newName o newPermissionsSet) debe ser proporcionado.");
        }
    }

    // Getters opcionales limpios (Nota: el record ya genera newName() y newPermissionsSet())
    public Optional<String> getNewNameOpt() {
        return Optional.ofNullable(newName);
    }

    public Optional<Set<UUID>> getNewPermissionsSetOpt() {
        return Optional.ofNullable(newPermissionsSet);
    }
}