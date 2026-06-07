package com.backwell.api_service.common.exception.codes;

public enum ItemErrorCode  implements ErrorCodeEnum {
    ITEM_NOT_FOUND,
    CREATION_ATTRIBUTES_MISMATCH,
    ;

    @Override
    public String getMessage() {
        return this.name().replace("_", " ");
    }
}
