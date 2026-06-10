package com.backwell.api_service.common.exception.codes;


import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

public enum ProductErrorCode implements ErrorCodeEnum {

    /*  Category Error Codes*/
    CATEGORY_ALREADY_EXISTS(CONFLICT),
    INVALID_CATEGORY_GRAPH(UNPROCESSABLE_ENTITY),
    CATEGORY_DELETION_CONFLICT(CONFLICT),
    CATEGORY_NOT_FOUND(NOT_FOUND),

    /*  Product Error Codes*/
    PRODUCT_NAME_CONFLICT(CONFLICT),
    PRODUCT_NOT_FOUND(NOT_FOUND),

    /*  Item Error Codes*/
    ITEM_NOT_FOUND(NOT_FOUND),
    STOCK_CONFLICT(CONFLICT),


    ;

    private final HttpStatus httpStatus;
    ProductErrorCode() {
        this.httpStatus = BAD_REQUEST;
    }
    ProductErrorCode(HttpStatus httpStatus) {
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
