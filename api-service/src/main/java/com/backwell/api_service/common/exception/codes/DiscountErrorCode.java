package com.backwell.api_service.common.exception.codes;

public enum DiscountErrorCode  implements ErrorCodeEnum{
    DISCOUNT_NOT_FOUND,
    NOT_UPDATABLE_DISCOUNT,
    INVALID_DATE_RANGE,
    DISCOUNT_ALREADY_STARTED;


    @Override
    public String getMessage() {
        return this.name().replace("_", " ");
    }
}
