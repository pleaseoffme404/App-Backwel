package com.backwell.api_service.modules.invitations.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Data
@Validated
@Configuration
@ConfigurationProperties("app.invitation.commission")
public class InvitationCommissionProperties {

    @NotNull
    private CalculationStrategy calculationStrategy;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal percentage;

    @DecimalMin("1.0")
    @DecimalMax("2.0")
    private BigDecimal aFactor;

    @Valid
    private List<@NotNull CommissionTier> tiers = new ArrayList<>();

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal upperLimitPercentage;

    @PostConstruct
    public void setUp() {
        if (tiers == null || tiers.isEmpty()) return;

        tiers.sort(Comparator.comparing(CommissionTier::upperLimit));
    }

    @AssertTrue(message = "Configuración de comisión inválida para la estrategia seleccionada")
    public boolean isValid() {
        return switch (calculationStrategy) {
            case LINEAR -> Objects.nonNull(percentage);

            case LOGARITHMIC -> Objects.nonNull(percentage)
                    && Objects.nonNull(aFactor)
                    && isAFactorInValidIncrements();

            case PIECEWISE_LINEAR -> Objects.nonNull(tiers)
                    && !tiers.isEmpty()
                    && !Objects.nonNull(upperLimitPercentage);
        };
    }

    private boolean isAFactorInValidIncrements() {
        BigDecimal quantum = new BigDecimal("0.25");
        for(int i = 4; i < 9; i++) {
            if (aFactor.compareTo(quantum.multiply(BigDecimal.valueOf(i))) == 0) {
                return true;
            }
        }
        return false;
    }
}