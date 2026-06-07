package com.backwell.api_service.modules.discount.dto;

import com.backwell.api_service.common.exception.SystemException;

import java.math.BigDecimal;
import java.util.Objects;
public record CategoryDiscountExtract(
        boolean hasDiscount,
        BigDecimal decimalFactor,
        BigDecimal percentage
) {

    private static final BigDecimal MAX_DISCOUNT_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static CategoryDiscountExtract noDiscount() {
        return new CategoryDiscountExtract(false, BigDecimal.ONE, BigDecimal.ZERO);
    }

    public static CategoryDiscountExtract of(BigDecimal decimalValue) {
        if (decimalValue == null || decimalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return noDiscount();
        }

        checkDecimalValueRange(decimalValue);

        BigDecimal percentage = decimalValue.multiply(HUNDRED);
        BigDecimal decimalFactor = BigDecimal.ONE.subtract(decimalValue);

        return new CategoryDiscountExtract(true, decimalFactor, percentage);
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
}
