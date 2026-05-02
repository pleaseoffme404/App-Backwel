package com.backwell.api_service.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private UUID traceId;
    private int status;
    private String code;
    private String message;
    private Instant timestamp;
    private String path;
    private Map<String, String> validationErrors;

    @Builder
    public ApiError(
            UUID traceId,
            int status,
            String code,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        this.traceId = traceId;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.validationErrors = validationErrors;

        this.timestamp = Instant.now();

    }
}
