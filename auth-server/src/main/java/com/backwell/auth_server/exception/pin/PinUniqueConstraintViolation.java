package com.backwell.auth_server.exception.pin;

public class PinUniqueConstraintViolation extends RuntimeException {
    public PinUniqueConstraintViolation(String message) {
        super(message);
    }
}
