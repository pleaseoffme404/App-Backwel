package com.backwell.auth_server.exception.role;

public class RoleNameConflictException extends RuntimeException {
    public RoleNameConflictException(String message) {
        super(message);
    }
}
