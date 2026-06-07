package com.backwell.api_service.common.exception.codes;

public enum UserErrorCode implements ErrorCodeEnum {
    USER_NOT_FOUND,
    ACCOUNT_COMPLETED,
    USER_ALREADY_EXISTS,

    // Referral Related Error Codes
    INVITATION_FAILED,


    /*  Address Related Error Codes*/
    ADDRESS_NOT_FOUND,
    ADDRESS_REORDER_CONFLICT,
    MAX_ADDRESS_LIMIT_REACHED;


    @Override
    public String getMessage() {
        return this.name().replaceAll("_", "-");
    }
}
