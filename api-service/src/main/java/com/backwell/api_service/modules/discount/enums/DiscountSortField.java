package com.backwell.api_service.modules.discount.enums;

import com.backwell.api_service.modules.discount.jpa.entity.Discount_;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum DiscountSortField {
    DISCOUNT_ID(Discount_.ID),
    NAME(Discount_.NAME),
    DISCOUNT_DECIMAL(Discount_.DECIMAL_VALUE),
    CREATED_AT(Discount_.CREATED_AT),;


    @Getter
    private final String attribute;

    private static final DiscountSortField[] allValues = values();

    public static DiscountSortField fromStringOrDefault(String source) {
        if (source == null || source.isEmpty()) {
            return CREATED_AT;
        }

        for (DiscountSortField value : allValues) {
            if (value.name().equalsIgnoreCase(source)) {
                return value;
            }
        }
        // return default
        return CREATED_AT;
    }
}
