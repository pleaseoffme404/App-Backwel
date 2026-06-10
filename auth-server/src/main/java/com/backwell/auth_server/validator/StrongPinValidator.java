package com.backwell.auth_server.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;


public class StrongPinValidator implements ConstraintValidator<StrongPin, String> {

    @Override
    public boolean isValid(String pin, ConstraintValidatorContext context) {
        if (pin == null || !pin.matches("^\\d{6}$")) return false;

        if (isSequential(pin)) {
            return failWith(context, "El PIN no puede ser una secuencia numérica (ej. 123456, 654321).");
        }
        if (isRepeated(pin)) {
            return failWith(context, "El PIN no puede estar compuesto por dígitos repetidos (ej. 111111, 222222).");
        }
        if (isCommonPin(pin)) {
            return failWith(context, "El PIN es demasiado común. Elige uno menos predecible.");
        }
        if (hasSingleUniqueDigit(pin)) {
            return failWith(context, "El PIN debe contener al menos 3 dígitos distintos.");
        }

        return true;
    }

    // 123456, 234567, 654321, etc.
    private boolean isSequential(String pin) {
        int step = pin.charAt(1) - pin.charAt(0);
        if (step != 1 && step != -1) return false;
        for (int i = 1; i < pin.length(); i++) {
            if ((pin.charAt(i) - pin.charAt(i - 1)) != step) return false;
        }
        return true;
    }

    // 111111, 333333, etc.
    private boolean isRepeated(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    // Menos de 3 dígitos únicos: 112233, 111222, etc.
    private boolean hasSingleUniqueDigit(String pin) {
        return pin.chars().distinct().count() < 3;
    }

    // Lista negra de PINs estadísticamente comunes
    private boolean isCommonPin(String pin) {
        return CommonPins.LIST.contains(pin);
    }

    private boolean failWith(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}