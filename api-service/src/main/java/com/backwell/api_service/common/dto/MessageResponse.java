package com.backwell.api_service.common.dto;

import java.time.Instant;

public record MessageResponse (
        String message,
        Instant timestamp,
        String path,
        int status
) {
    public MessageResponse(String message, String path, int status) {
        this(message, Instant.now(), path, status);
    }
}
