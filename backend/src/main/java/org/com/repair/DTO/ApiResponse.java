package org.com.repair.DTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String timestamp
) {

    @JsonProperty("errorCode")
    public String getErrorCode() {
        return code;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", "OK", data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, "CREATED", message, data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, LocalDateTime.now().toString());
    }
}
