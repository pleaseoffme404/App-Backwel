package com.backwell.api_service.modules.invitations.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record CommissionTier(
        @DecimalMin("0.0")
        BigDecimal upperLimit,

        @DecimalMin("0.0")
        @DecimalMax("80")
        BigDecimal percentage
) {
}
