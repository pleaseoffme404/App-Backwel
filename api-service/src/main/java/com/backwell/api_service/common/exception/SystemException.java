package com.backwell.api_service.common.exception;

import org.springframework.http.HttpStatus;

public class SystemException extends BaseException {

    public SystemException(String message) {
        super(message, "INTERNAL SERVER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
