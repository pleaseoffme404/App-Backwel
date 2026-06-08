package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

public enum UserErrorCode implements ErrorCodeEnum {
    USER_NOT_FOUND(NOT_FOUND),
    ACCOUNT_COMPLETED(CONFLICT),
    USER_ALREADY_EXISTS(CONFLICT),

    // Referral Related Error Codes
    INVITATION_FAILED,


    /*  Address Related Error Codes*/
    ADDRESS_NOT_FOUND(NOT_FOUND),
    ADDRESS_REORDER_CONFLICT(CONFLICT),
    MAX_ADDRESS_LIMIT_REACHED;

    private final HttpStatus errorCode;

    UserErrorCode() {
        this.errorCode = BAD_REQUEST;
    }
    UserErrorCode(HttpStatus errorCode) {
        this.errorCode = errorCode;
    }


    @Override
    public String getMessage() {
        return this.name().replace("_", "-");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return this.errorCode;
    }

}
