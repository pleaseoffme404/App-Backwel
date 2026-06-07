package com.backwell.api_service.modules.discount.controller.req;

import com.backwell.api_service.validators.AtLeastOneNotNull;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@AtLeastOneNotNull
public record UpdateDiscountRequest(

        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        @Pattern(
                regexp = "^(?=[\\s\\S]*\\S)[\\s\\S]{1,255}$",
                message = "El nombre tiene caracteres no permitidos o está vacío"
        )
        String name,

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        @Digits(integer = 1, fraction = 2)
        BigDecimal decimalValue,

        Boolean active,
        Boolean stackable,
        Instant startDate,
        Instant endDate
) {
    @AssertTrue(message = "La fecha de inicio debe ser anterior a la de fin y no puede ser del pasado")
    public boolean isValidDates() {
        if (startDate == null && endDate == null) {
            return true;
        }

        Instant laxNow = Instant.now().minusSeconds(10);
        if (startDate != null && startDate.isBefore(laxNow)) {
            return false;
        }

        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }

    public Optional<String> nameOptional() {
        return Optional.ofNullable(name);
    }

    public Optional<BigDecimal> decimalValueOptional() {
        return Optional.ofNullable(decimalValue);
    }

    public Optional<Boolean> activeOptional() {
        return Optional.ofNullable(active);
    }

    public Optional<Boolean> stackableOptional() {
        return Optional.ofNullable(stackable);
    }

    public Optional<Instant> startDateOptional() {
        return Optional.ofNullable(startDate);
    }

    public Optional<Instant> endDateOptional() {
        return Optional.ofNullable(endDate);
    }
}
