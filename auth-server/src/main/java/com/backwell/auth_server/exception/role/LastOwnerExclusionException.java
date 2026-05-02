package com.backwell.auth_server.exception.role;

public class LastOwnerExclusionException extends RuntimeException {
    public LastOwnerExclusionException(String message) {
        super(message);
    }
}
