package org.com.repair.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> handleGamificationException(GamificationException ex) {
        HttpStatus status = mapGamificationStatus(ex.getErrorCode());
        return buildResponse(status, ex.getMessage(), ex.getErrorCode().name());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数校验失败");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "数据冲突，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后再试");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return buildResponse(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        if (errorCode != null) {
            body.put("errorCode", errorCode);
        }
        return new ResponseEntity<>(body, status);
    }

    private HttpStatus mapGamificationStatus(GamificationErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CITY_INDEX, QUIZ_NOT_FOUND, QUIZ_CITY_MISMATCH, JOURNEY_NOT_COMPLETED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.CONFLICT;
        };
    }
}
