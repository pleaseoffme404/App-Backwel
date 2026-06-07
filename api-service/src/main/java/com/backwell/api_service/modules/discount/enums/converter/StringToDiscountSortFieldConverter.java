package com.backwell.api_service.modules.discount.enums.converter;

import com.backwell.api_service.modules.discount.enums.DiscountSortField;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDiscountSortFieldConverter implements Converter<String, DiscountSortField> {
    @Nullable
    @Override
    public DiscountSortField convert(String source) {
        return DiscountSortField.fromStringOrDefault(source);
    }
}
