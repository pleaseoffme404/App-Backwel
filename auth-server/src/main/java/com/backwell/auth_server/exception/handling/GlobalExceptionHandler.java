package com.backwell.auth_server.exception.handling;

import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.exception.pin.AccountLockedException;
import com.backwell.auth_server.exception.pin.InvalidPinException;
import com.backwell.auth_server.exception.pin.PinUniqueConstraintViolation;
import com.backwell.auth_server.exception.pin.PinVerificationInProgressException;
import com.backwell.auth_server.exception.role.UnknownPermissionException;
import com.backwell.auth_server.exception.role.UnknownRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<MessageResponse> handleInvalidPinException(InvalidPinException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<MessageResponse> handleAccountLockedException(AccountLockedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(PinVerificationInProgressException.class)
    public ResponseEntity<MessageResponse> handlePinVerificationInProgressException(PinVerificationInProgressException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(PinUniqueConstraintViolation.class)
    public ResponseEntity<MessageResponse> handlePinUniqueConstraintViolation(PinUniqueConstraintViolation ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnknownRoleException.class)
    public ResponseEntity<MessageResponse> handleUnknownRoleException(UnknownRoleException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(ex.getMessage()));
    }
    @ExceptionHandler(UnknownPermissionException.class)
    public ResponseEntity<MessageResponse> handleUnknownPermissionException(UnknownPermissionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(ex.getMessage()));
    }
}
