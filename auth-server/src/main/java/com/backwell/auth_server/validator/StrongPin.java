package com.backwell.auth_server.validator;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPinValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPin {
    String message() default "El PIN es demasiado débil o predecible.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
