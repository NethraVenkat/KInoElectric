package com.backend.javaleap.model.response;

import java.time.LocalDateTime;

/** API key metadata - never contains the key itself. */
public record ApiKeyResponse(
        Long id,
        String name,
        String keyPrefix,
        String actsAsUser,
        boolean enabled,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt) {
}
