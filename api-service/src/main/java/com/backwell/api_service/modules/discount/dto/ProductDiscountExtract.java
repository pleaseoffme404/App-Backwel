package com.backwell.api_service.modules.discount.dto;

import com.backwell.api_service.common.exception.SystemException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record ProductDiscountExtract(
        boolean hasDiscount,
        BigDecimal decimalFactor,
        BigDecimal percentage
) {
    private static final BigDecimal MAX_DISCOUNT_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static ProductDiscountExtract noDiscount() {
        return new ProductDiscountExtract(false, BigDecimal.ONE, BigDecimal.ZERO);
    }

    public static ProductDiscountExtract of(BigDecimal decimalValue) {
        if (decimalValue == null || decimalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return noDiscount();
        }

        checkDecimalValueRange(decimalValue);

        BigDecimal percentage = decimalValue.multiply(HUNDRED);
        BigDecimal decimalFactor = BigDecimal.ONE.subtract(decimalValue);

        return new ProductDiscountExtract(true, decimalFactor, percentage);
    }

    private static void checkDecimalValueRange(BigDecimal val) {
        Objects.requireNonNull(val, "Discount value cannot be null");

        if (val.compareTo(BigDecimal.ZERO) < 0) {
            throw new SystemException("Discount Decimal Value cannot be less than zero");
        }

        if (val.compareTo(MAX_DISCOUNT_THRESHOLD) > 0) {
            throw new SystemException("Discount cannot exceed 80% (0.8)");
        }
    }

    public BigDecimal finalPrice(BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return basePrice.multiply(this.decimalFactor).setScale(2, RoundingMode.HALF_UP);
    }
}
