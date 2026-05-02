package com.backwell.api_service.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {
    private static final Set<String> ISO_3166_1_A2_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if  (value == null) return true;

        return ISO_3166_1_A2_COUNTRY_CODES.contains(value.toUpperCase());
    }
}
