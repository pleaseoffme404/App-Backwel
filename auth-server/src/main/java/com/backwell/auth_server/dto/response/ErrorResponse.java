package com.backwell.auth_server.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> errors

) {
    public ErrorResponse (int status, String message, Map<String, String> errors) {
        this (status, message, Instant.now(), errors);
    }
}
