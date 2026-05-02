package com.backwell.auth_server.exception;

public class AuthProviderMismatchException extends RuntimeException {
    public AuthProviderMismatchException(String message) {
        super(message);
    }
}
