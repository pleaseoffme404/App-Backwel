package com.backwell.auth_server.exception.role;

public class RoleEscalationDeniedException extends RuntimeException {

    public RoleEscalationDeniedException(String message) {
        super(message);
    }
}
