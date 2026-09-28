package com.backend.javaleap.model.response;

import java.time.Instant;

/** Returned after a successful login. Send accessToken as "Authorization: Bearer ...". */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        Instant expiresAt,
        UserResponse user) {
}
