package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.validators.ValidCountryCode;
import com.backwell.api_service.validators.ValidCurrencyCode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;
import java.util.stream.Stream;

public record UpdateUserInfoRequest(

        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Name contains forbidden characters.")
        @Size(max = 100)
        String name,
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Surname contains forbidden characters.")
        @Size(max = 100)
        String surname,

        @Size(max = 14)
        String phoneNumber,

        @ValidCountryCode
        String countryCode,

        @ValidCurrencyCode
        String currencyCode,

        @URL
        @Size(max = 2048, message = "Picture URL can not be longer than 2048 chars.")
        String pictureUrl
) {

    @Deprecated
    public UpdateUserInfoRequest {
        name = sanitize(name);
        surname = sanitize(surname);
        phoneNumber = sanitize(phoneNumber);
        pictureUrl = sanitize(pictureUrl);

        boolean hasAnyContent = Stream.of(name, surname, phoneNumber, pictureUrl)
                .anyMatch(Objects::nonNull);

        if (!hasAnyContent) {
            throw new IllegalArgumentException("Debe proporcionar al menos un campo válido para actualizar.");
        }

        validateLength(name, 50, "nombre");
        validateLength(surname, 50, "apellido");
        validateLength(phoneNumber, 20, "teléfono");
        validateLength(pictureUrl, 500, "URL de imagen");
    }

    private static String sanitize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validateLength(String field, int max, String fieldName) {
        if (field != null && field.length() > max) {
            throw new IllegalArgumentException("El campo " + fieldName + " excede el máximo de " + max + " caracteres.");
        }
    }
}