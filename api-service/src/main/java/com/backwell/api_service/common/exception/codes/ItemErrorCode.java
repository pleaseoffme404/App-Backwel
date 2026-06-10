package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;
import static  org.springframework.http.HttpStatus.*;

public enum ItemErrorCode  implements ErrorCodeEnum {
    ITEM_NOT_FOUND(NOT_FOUND),
    CREATION_ATTRIBUTES_MISMATCH(CONFLICT),
    ;

    private final HttpStatus httpStatus;

    ItemErrorCode() {
        this.httpStatus = BAD_REQUEST;
    }
    ItemErrorCode(HttpStatus httpStatus) {
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
