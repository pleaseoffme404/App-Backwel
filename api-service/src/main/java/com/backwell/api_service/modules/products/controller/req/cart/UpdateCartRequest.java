package com.backwell.api_service.modules.products.controller.req.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCartRequest(
        @NotNull UUID itemId,
        @NotNull Integer delta
) {
        public UpdateCartRequest {
                if (delta != 1 && delta != -1) {
                        throw new IllegalArgumentException("El delta debe ser estrictamente 1 o -1");
                }
        }

}
