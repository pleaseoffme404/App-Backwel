package com.backwell.auth_server.exception.pin;

public class PinVerificationInProgressException extends RuntimeException {
    public PinVerificationInProgressException(String message) {
        super(message);
    }
}
