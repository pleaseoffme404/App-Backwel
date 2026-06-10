package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

public enum OrderErrorCode implements ErrorCodeEnum {
    CART_ITEMS_CONFLICT(CONFLICT),
    EMPTY_CART(UNPROCESSABLE_ENTITY),
    ;

    private final HttpStatus httpStatus;

    OrderErrorCode(){
        this.httpStatus = BAD_REQUEST;
    }

    OrderErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return ErrorCodeEnum.super.getHttpStatus();
    }

}
