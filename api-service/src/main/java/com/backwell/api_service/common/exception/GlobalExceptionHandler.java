package com.backwell.api_service.common.exception;

import com.backwell.api_service.common.exception.dto.ApiError;
import com.backwell.api_service.common.exception.persistence.ErrorLogService;
import com.backwell.api_service.common.util.UUIDService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ErrorLogService errorLogService;
    private final UUIDService uuidService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((f) -> {
            errors.put(f.getField(), f.getDefaultMessage());
        });
        HttpStatus status = HttpStatus.BAD_REQUEST;

        UUID traceId = uuidService.next();
        var response = ApiError.builder()
                .traceId(traceId)
                .status(status.value())
                .code("VALIDATION_ERROR")
                .message("Campos Inválidos")
                .validationErrors(errors)
                .path(request.getRequestURI())
                .build();

        errorLogService.saveErrorLog(traceId, request, ex);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        UUID traceId = uuidService.next();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        var response = ApiError.builder()
                .traceId(traceId)
                .status(status.value())
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        errorLogService.saveErrorLog(traceId, request, ex);
        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintErrors(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage())
        );
        HttpStatus status = HttpStatus.BAD_REQUEST;

        UUID traceId = uuidService.next();
        var response = ApiError.builder()
                .traceId(traceId)
                .status(status.value())
                .code("CONSTRAINT_VIOLATION")
                .message("Parámetros Inválidos")
                .validationErrors(errors)
                .path(request.getRequestURI())
                .build();
        errorLogService.saveErrorLog(traceId, request, ex);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(YouAreAnIdiotException.class)
    public void handleStupidRequest(YouAreAnIdiotException ex, HttpServletResponse response) throws IOException {
        response.sendRedirect("https://youareanidiot.cc/");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) throws IOException {
        UUID traceId = uuidService.next();
        var response = ApiError.builder()
                .traceId(traceId)
                .status(ex.getHttpStatus().value())
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        errorLogService.saveErrorLog(traceId, request, ex);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ApiError> handleSystemException(SystemException ex, HttpServletRequest request) throws IOException {
        UUID traceId = uuidService.next();
        var response = ApiError.builder()
                .traceId(traceId)
                .status(ex.getHttpStatus().value())
                .code("INTERNAL_SERVER_ERROR")
                .message("Something went wrong with our servers. Please try cocks again")
                .path(request.getRequestURI())
                .build();
        errorLogService.saveErrorLog(traceId, request, ex);
        return ResponseEntity.internalServerError().body(response);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) throws IOException {
        UUID traceId = uuidService.next();

        var response = ApiError.builder()
                .traceId(traceId)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("UNEXPECTED_ERROR")
                .message("Unexpected Error Occurred. Contact Support for further information for log with trace: %s".formatted(traceId))
                .path(request.getRequestURI())
                .build();
        errorLogService.saveErrorLog(traceId, request, ex);
        return ResponseEntity.internalServerError().body(response);
    }
}
