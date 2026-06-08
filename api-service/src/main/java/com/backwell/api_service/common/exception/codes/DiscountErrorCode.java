package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

public enum DiscountErrorCode  implements ErrorCodeEnum{
    DISCOUNT_NOT_FOUND(NOT_FOUND),
    NOT_UPDATABLE_DISCOUNT(CONFLICT),
    INVALID_DATE_RANGE,
    DISCOUNT_ALREADY_STARTED,

    ;

    private final HttpStatus httpStatus;
    DiscountErrorCode() {
        this.httpStatus = BAD_REQUEST;
    }
    DiscountErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }


    @Override
    public String getMessage() {
        return this.name().replace("_", " ");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }


}
