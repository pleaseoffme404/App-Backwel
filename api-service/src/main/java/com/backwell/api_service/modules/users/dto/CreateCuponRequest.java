package com.backwell.api_service.modules.users.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

public record CreateCuponRequest (
        @NotBlank
        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Cupon Name contains forbidden characters.")
        String name,

        @NotNull
        boolean stackable,

        @DecimalMin("0.0")
        @DecimalMax("60.0")
        BigDecimal percentage,

        @NotNull
        @NotEmpty
        Set<UUID> targets
){

        public BigDecimal getDecimalFactor(){
                return BigDecimal.ONE.subtract(
                        percentage.divide(
                                new BigDecimal(100),
                                32,
                                RoundingMode.HALF_UP
                        )
                );
        }
}
