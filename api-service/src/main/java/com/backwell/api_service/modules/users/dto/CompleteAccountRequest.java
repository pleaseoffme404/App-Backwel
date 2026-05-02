package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.validators.ValidCountryCode;
import com.backwell.api_service.validators.ValidCurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.Currency;

public record CompleteAccountRequest(
        @NotBlank
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Name contains forbidden characters.")
        @Size(max = 100)
        String name,

        @NotBlank
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Surname contains forbidden characters.")
        @Size(max = 100)
        String surname,

        @NotNull
        @Size(max = 14)
        String phoneNumber,

        @NotNull
        @ValidCountryCode
        @Size(max = 2)
        String countryCode,

        @NotNull
        @ValidCurrencyCode
        @Size(max = 3)
        String currencyCode,

        @URL
        @Size(max = 2048)
        String avatarUrl,


        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Address Name contains forbidden characters.")
        String addressName,

        @NotNull
        @Valid
        CreateAddressDTO firstAddress
) { }
