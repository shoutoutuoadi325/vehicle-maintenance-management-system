package org.com.repair.controller;

import java.time.LocalDateTime;
import java.util.Objects;

import org.com.repair.DTO.ApiResponse;
import org.com.repair.exception.GamificationErrorCode;
import org.com.repair.exception.GamificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GamificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleGamificationException(GamificationException ex) {
        HttpStatus status = resolveGamificationStatus(ex.getErrorCode());
        String errorCode = ex.getErrorCode() == null ? "GAMIFICATION_ERROR" : ex.getErrorCode().name();
        return buildResponse(status, ex.getMessage(), errorCode, false);
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "请求处理失败";
        }
        return buildResponse(HttpStatus.BAD_REQUEST, message, "BUSINESS_ERROR");
    }

    @ExceptionHandler(GreenEnergyAccountProvisioningService.ConcurrentAccountCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentAccountCreation(
            GreenEnergyAccountProvisioningService.ConcurrentAccountCreationException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "IDEMPOTENCY_CONFLICT");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        logger.error("Unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试", "INTERNAL_ERROR");
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message, String errorCode) {
        return buildResponse(status, message, errorCode, true);
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status,
                                                            String message,
                                                            String errorCode,
                                                            boolean formatWithApiPrefix) {
        String normalizedCode = normalizeBusinessCode(errorCode);
        ApiResponse<Void> body = new ApiResponse<>(
                false,
                formatWithApiPrefix ? formatErrorCode(status, normalizedCode) : normalizedCode,
                message,
                null,
                LocalDateTime.now().toString());
        return new ResponseEntity<>(body, Objects.requireNonNull(status));
    }

    private String formatErrorCode(HttpStatus status, String businessCode) {
        return "API_" + status.value() + "_" + normalizeBusinessCode(businessCode);
    }

    private String normalizeBusinessCode(String businessCode) {
        return businessCode == null ? "UNKNOWN" : businessCode.trim().replace(' ', '_').toUpperCase();
    }

    private HttpStatus resolveGamificationStatus(GamificationErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (errorCode) {
            case NODE_ALREADY_CHECKED_IN,
                 NODE_NOT_UNLOCKED,
                 NODE_NOT_CURRENT_UNLOCKED,
                 DAILY_QUIZ_LIMIT_REACHED,
                 REWARD_ALREADY_GRANTED,
                 DAILY_ENERGY_CAP_EXCEEDED,
                 MAP_SELECTION_LOCKED,
                 RANDOM_EVENT_PENDING,
                 RANDOM_EVENT_NOT_PENDING,
                 RANDOM_EVENT_QUIZ_MISMATCH,
                 COUPON_NOT_REDEEMABLE,
                 RETRY_COOLDOWN_ACTIVE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
