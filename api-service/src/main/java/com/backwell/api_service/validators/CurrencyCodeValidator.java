package com.backwell.api_service.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;

public class CurrencyCodeValidator  implements ConstraintValidator<ValidCurrencyCode, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            return Currency.getInstance(value.toUpperCase()) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
