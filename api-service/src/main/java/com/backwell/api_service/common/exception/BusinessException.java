package com.backwell.api_service.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    public BusinessException(String message, String errorCode) {
      super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}
