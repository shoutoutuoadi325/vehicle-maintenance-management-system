package org.com.repair.controller;

import java.time.LocalDateTime;
import java.util.Objects;

import org.com.repair.DTO.ApiResponse;
import org.com.repair.exception.GamificationException;
import org.com.repair.service.GreenEnergyAccountProvisioningService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GamificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleGamificationException(GamificationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "GAMIFICATION_INVALID_REQUEST");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "IDEMPOTENCY_OR_STATE_CONFLICT");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "FORBIDDEN");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数校验失败");
        return buildResponse(HttpStatus.BAD_REQUEST, message, "VALIDATION_FAILED");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "数据冲突，请勿重复提交", "DATA_CONFLICT");
    }

    @ExceptionHandler(GreenEnergyAccountProvisioningService.ConcurrentAccountCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentAccountCreation(
            GreenEnergyAccountProvisioningService.ConcurrentAccountCreationException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "IDEMPOTENCY_CONFLICT");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试", "INTERNAL_ERROR");
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message, String errorCode) {
        ApiResponse<Void> body = new ApiResponse<>(
                false,
                formatErrorCode(status, errorCode),
                message,
                null,
                LocalDateTime.now().toString());
        return new ResponseEntity<>(body, Objects.requireNonNull(status));
    }

    private String formatErrorCode(HttpStatus status, String businessCode) {
        String normalized = businessCode == null ? "UNKNOWN" : businessCode.trim().replace(' ', '_').toUpperCase();
        return "API_" + status.value() + "_" + normalized;
    }
}
