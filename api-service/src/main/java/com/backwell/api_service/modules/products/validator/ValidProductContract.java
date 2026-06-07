package com.backwell.api_service.modules.products.validator;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductContractValidator.class)
@Documented
public @interface ValidProductContract {
    String message() default "El producto no cumple con el contrato de atributos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
