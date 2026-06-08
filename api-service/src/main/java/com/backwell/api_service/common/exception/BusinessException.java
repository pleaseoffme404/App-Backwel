package com.backwell.api_service.common.exception;

import com.backwell.api_service.common.exception.codes.ErrorCodeEnum;
import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {
    public BusinessException(String message, String errorCode) {
      super(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, ErrorCodeEnum errorCode) {
        super (message, errorCode.getMessage(), errorCode.getHttpStatus());
    }
}
