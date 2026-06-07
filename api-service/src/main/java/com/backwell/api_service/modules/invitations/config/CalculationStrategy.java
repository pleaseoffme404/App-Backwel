package com.backwell.api_service.modules.invitations.config;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import ch.obermuhlner.math.big.BigDecimalMath;

public enum CalculationStrategy {


    LINEAR {
        @Override
        public BigDecimal calculate(BigDecimal amount, InvitationCommissionProperties properties) {
            return amount.multiply(properties.getPercentage())
                    .divide(BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP
                    );
        }
    },
    LOGARITHMIC {
        @Override
        public BigDecimal calculate(BigDecimal x, InvitationCommissionProperties properties) {
            BigDecimal a = properties.getAFactor();
            BigDecimal k = properties.getPercentage();

            MathContext mc = new MathContext(32, RoundingMode.HALF_UP);

            BigDecimal factor = k.multiply(BigDecimalMath.pow(BigDecimal.TEN, a, mc));
            BigDecimal divider = BigDecimalMath.pow(BigDecimal.TEN, a.add(BigDecimal.TWO), mc);
            BigDecimal logArgument = BigDecimal.ONE.add(x.divide(divider, mc));
            return factor.multiply(BigDecimalMath.log(logArgument, mc)).setScale(2,RoundingMode.HALF_UP);
        }

    },
    PIECEWISE_LINEAR {
        @Override
        public BigDecimal calculate(BigDecimal amount, InvitationCommissionProperties properties) {
            // considering this is a crescendo
            List<CommissionTier> tiers = properties.getTiers();
            MathContext mc = new MathContext(32, RoundingMode.HALF_UP);

            for (CommissionTier tier : tiers) {
                if (amount.compareTo(tier.upperLimit()) <= 0) {
                    return amount.multiply(tier.percentage())
                            .divide(new BigDecimal("100"), mc)
                            .setScale(2,RoundingMode.HALF_UP);
                }
            }
            return amount.multiply(properties.getUpperLimitPercentage())
                    .divide(new BigDecimal("100"), mc)
                    .setScale(2,RoundingMode.HALF_UP);
        }
    };

    /**
     * Calculate the commission for a purchase
     * @return BigDecimal fixed to 2 decimal places Half Up Rounded*/
    public abstract BigDecimal calculate(BigDecimal amount, InvitationCommissionProperties properties);
}
