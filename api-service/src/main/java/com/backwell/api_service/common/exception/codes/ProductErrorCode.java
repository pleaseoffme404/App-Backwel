package com.backwell.api_service.common.exception.codes;


public enum ProductErrorCode implements ErrorCodeEnum {

    /*  Category Error Codes*/
    CATEGORY_ALREADY_EXISTS,
    INVALID_CATEGORY_GRAPH,
    CATEGORY_DELETION_CONFLICT,
    CATEGORY_NOT_FOUND,

    /*  Product Error Codes*/
    PRODUCT_NAME_CONFLICT,
    PRODUCT_NOT_FOUND,

    /*  Item Error Codes*/
    ITEM_NOT_FOUND,
    STOCK_CONFLICT,


    ;


    @Override
    public String getMessage() {
        return this.name().replace("_", " ");
    }
}
