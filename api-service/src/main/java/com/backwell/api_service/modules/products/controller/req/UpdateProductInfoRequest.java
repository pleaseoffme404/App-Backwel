package com.backwell.api_service.modules.products.controller.req;

import com.backwell.api_service.validators.AtLeastOneNotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@AtLeastOneNotNull
public record UpdateProductInfoRequest(
        @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]+$",
                message = "El nombre contiene caracteres no permitidos")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]*$",
                message = "La descripción contiene caracteres no permitidos")
        String description,

        @Size(max = 255, message = "La marca no puede superar los 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]+$",
                message = "La marca contiene caracteres no permitidos")
        String brand,

        Map<UUID, String> attributeKeyNames
) {

    private static final java.util.regex.Pattern ALLOWED_CHARS_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s\\.,;:\\-\\!\\?¿¡\\(\\)\"]+$");

    /**
     * Constructor compacto para validaciones avanzadas del mapa y asegurar inmutabilidad deep.
     */
    public UpdateProductInfoRequest {

        if (attributeKeyNames != null) {
            Set<String> seenValues = new HashSet<>();

            for (Map.Entry<UUID, String> entry : attributeKeyNames.entrySet()) {
                String value = getString(entry);

                // 3. Validar caracteres permitidos
                if (!ALLOWED_CHARS_PATTERN.matcher(value).matches()) {
                    throw new IllegalArgumentException("El valor '%s' contiene caracteres no permitidos".formatted(value));
                }

                // 4. Validar que no haya valores String duplicados
                if (!seenValues.add(value)) {
                    throw new IllegalArgumentException("El mapa contiene valores duplicados: '%s'".formatted(value));
                }
            }

            attributeKeyNames = Map.copyOf(attributeKeyNames);
        }
    }

    @NotNull
    private static String getString(Map.Entry<UUID, String> entry) {
        UUID key = entry.getKey();
        String value = entry.getValue();

        if (key == null || value == null) {
            throw new IllegalArgumentException("El mapa attributeKeyNames no puede contener llaves o valores nulos");
        }

        if (value.isBlank() || value.length() > 50) {
            throw new IllegalArgumentException("El valor '%s' debe tener entre 1 y 50 caracteres".formatted(value));
        }
        return value;
    }

    // Optional Getters

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getBrand() {
        return Optional.ofNullable(brand);
    }

    public Optional<Map<UUID, String>> getAttributeKeyNames() {
        return Optional.ofNullable(attributeKeyNames);
    }
}
