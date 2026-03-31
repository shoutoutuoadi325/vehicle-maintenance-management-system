package org.com.repair.controller;

import java.time.LocalDateTime;
import java.util.Objects;

import org.com.repair.DTO.ApiResponse;
import org.com.repair.exception.GamificationErrorCode;
import org.com.repair.exception.GamificationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GamificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleGamificationException(GamificationException ex) {
        HttpStatus status = mapGamificationStatus(ex.getErrorCode());
        return buildResponse(status, ex.getMessage(), ex.getErrorCode().name());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "BAD_REQUEST");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), "CONFLICT");
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试", "INTERNAL_ERROR");
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message, String errorCode) {
        ApiResponse<Void> body = new ApiResponse<>(
                false,
                errorCode,
                message,
                null,
                LocalDateTime.now().toString());
        return new ResponseEntity<>(body, Objects.requireNonNull(status));
    }

    private HttpStatus mapGamificationStatus(GamificationErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CITY_INDEX, QUIZ_NOT_FOUND, QUIZ_CITY_MISMATCH, JOURNEY_NOT_COMPLETED, INVALID_SHIPPING_STATUS,
                    MAP_NOT_FOUND, MAP_NOT_ACTIVE, MAP_SELECTION_LOCKED,
                    RANDOM_EVENT_NOT_PENDING, RANDOM_EVENT_QUIZ_MISMATCH,
                    COUPON_WALLET_NOT_FOUND, COUPON_NOT_REDEEMABLE, COUPON_EXPIRED,
                    REDEEM_OPERATOR_MISSING, REDEEM_SHOP_NOT_FOUND, REDEEM_TECHNICIAN_NOT_FOUND,
                    RETRY_COOLDOWN_ACTIVE -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
    }
}
