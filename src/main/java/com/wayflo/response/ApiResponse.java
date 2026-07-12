package com.wayflo.response;

import java.time.Instant;

public record ApiResponse<T>(
    Instant timestamp,
    T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(Instant.now(), data);
    }
}
