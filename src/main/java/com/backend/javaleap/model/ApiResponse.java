package com.backend.javaleap.model;

import java.time.Instant;

/**
 * Standard wrapper for every API response.
 * <pre>
 * { "success": true, "message": "OK", "data": {...}, "timestamp": "2026-09-27T10:15:30Z" }
 * </pre>
 *
 * @param <T> payload type
 */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message, T details) {
        return new ApiResponse<>(false, message, details, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
