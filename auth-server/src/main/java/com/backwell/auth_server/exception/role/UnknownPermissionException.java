package com.backwell.auth_server.exception.role;

public class UnknownPermissionException extends RuntimeException {
    public UnknownPermissionException(String message) {
        super(message);
    }
}
