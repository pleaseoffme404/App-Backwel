package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.validators.ValidCountryCode;
import com.backwell.api_service.validators.ValidCurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

public record CompleteAccountRequest(
        @NotBlank(message = "Name is required and cannot be empty.")
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Name contains invalid characters. Only letters, spaces, hyphens, and apostrophes are allowed.")
        @Size(max = 100, message = "Name cannot exceed {max} characters.")
        String name,

        @NotBlank(message = "Surname is required and cannot be empty.")
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Surname contains invalid characters. Only letters, spaces, hyphens, and apostrophes are allowed.")
        @Size(max = 100, message = "Surname cannot exceed {max} characters.")
        String surname,

        @NotBlank(message = "Phone number is required and cannot be empty.")
        @Size(min = 7, max = 14, message = "Phone number must be between {min} and {max} characters.")
        String phoneNumber,

        @NotBlank(message = "Country code is required and cannot be empty.")
        @ValidCountryCode(message = "Invalid country code format or region.")
        @Size(min = 2, max = 2, message = "Country code must be exactly {max} characters (ISO 3166-1 alpha-2).")
        String countryCode,

        @NotBlank(message = "Currency code is required and cannot be empty.")
        @ValidCurrencyCode(message = "Invalid currency code.")
        @Size(min = 3, max = 3, message = "Currency code must be exactly {max} characters (ISO 4217).")
        String currencyCode,

        @URL(message = "Avatar URL must be a valid absolute URL.")
        @Size(max = 2048, message = "Avatar URL cannot exceed {max} characters.")
        String avatarUrl,

        UUID invitationCode,

        @NotBlank(message = "Address name is required and cannot be empty.")
        @Size(min = 3, max = 100, message = "Address name must be between {min} and {max} characters.")
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Address name contains invalid characters.")
        String addressName,

        @NotNull(message = "First address information is required.")
        @Valid
        CreateAddressDTO firstAddress
) { }