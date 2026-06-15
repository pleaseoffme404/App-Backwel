package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

public enum CreditErrorCode implements ErrorCodeEnum{
    DUPLICATE_REQUEST(CONFLICT),
    NEGATIVE_BALANCE_ERROR(CONFLICT),
    PRECISION_LIMIT_EXCEEDED(CONFLICT),
    OPTIMISTIC_LOCK_ERROR(CONFLICT),
    ;


    private final HttpStatus httpStatus;

    CreditErrorCode(HttpStatus status){
        this.httpStatus = status;
    }

    CreditErrorCode(){
        this.httpStatus = HttpStatus.BAD_REQUEST;
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
