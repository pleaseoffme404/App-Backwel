package com.backwell.api_service.modules.products.controller.validator;

import com.backwell.api_service.modules.products.controller.dto.CreateItemDTO;
import com.backwell.api_service.modules.products.controller.dto.CreateProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProductContractValidator implements ConstraintValidator<ValidProductContract, CreateProductRequest> {
    @Override
    public boolean isValid(CreateProductRequest request, ConstraintValidatorContext context) {

        // let the record annotations manage null fields
        if  (request == null || request.items() == null || request.attributes() == null) {
            return true;
        }
        Set<String> contract = request.attributes();
        Set<Map<String, String>> seenAttributes = new HashSet<>();

        for (CreateItemDTO item : request.items()) {
            Map<String, String> itemAttrs = item.itemAttributes();

            // REGLA 1: El keyset debe ser igual al contrato del producto
            if (!itemAttrs.keySet().equals(contract)) {
                buildError(context, "Los atributos del item no coinciden con el contrato global del producto.");
                return false;
            }

            // REGLA 2: No pueden existir dos items con el mismo mapa de atributos
            // Map.equals() en Java compara contenido (llaves y valores)
            if (!seenAttributes.add(itemAttrs)) {
                buildError(context, "No se permiten items duplicados con la misma combinación de atributos.");
                return false;
            }
        }

        return true;
    }

    private void buildError(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
