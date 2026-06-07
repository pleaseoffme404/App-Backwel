package com.backwell.api_service.modules.discount.controller.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record CreateDiscountRequest (
        @NotBlank
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        @Pattern(
                regexp = "^(?=[\\s\\S]*\\S)[\\s\\S]{1,255}$",
                message = "El nombre tiene caracteres no permitidos o está vacío"
        )
        String discountName,

        boolean stackable,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        @Digits(integer = 1, fraction = 4)
        BigDecimal discountDecimal,

        @NotNull
        Instant startDate,

        @NotNull
        Instant endDate,

        @Valid
        @NotNull
        DiscountTargetsDTO targets
) {

    public CreateDiscountRequest {

        if (startDate == null || endDate == null || discountDecimal == null) {
            throw new IllegalArgumentException("startDate, endDate, and discountDecimal must not be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Discount's start date must be before end date");
        }

        Instant laxNow = Instant.now().minusSeconds(10);

        if (endDate.isBefore(laxNow)) {
            throw new IllegalArgumentException("Discount's end date must be in the future. Current time: %s".formatted(laxNow));
        }

        discountDecimal = discountDecimal.setScale(4, RoundingMode.HALF_UP);
    }
}
