package com.backwell.api_service.modules.invitations.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Validated
@Configuration
@ConfigurationProperties("app.invitation.discount")
public class InvitationDiscountProperties {

    @DecimalMin("0.0")
    @DecimalMax("50.0")
    private BigDecimal discountPercentage;

    public BigDecimal getDecimalFactor() {
        return BigDecimal.ONE.subtract(
                discountPercentage.divide(
                        BigDecimal.valueOf(100),
                        32,
                        RoundingMode.HALF_UP
                )
        );
    }
}
