package com.backwell.api_service.common.exception.codes;

import org.springframework.http.HttpStatus;

public interface ErrorCodeEnum {

    String getMessage();

    default HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
