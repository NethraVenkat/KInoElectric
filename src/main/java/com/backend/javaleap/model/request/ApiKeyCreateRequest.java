package com.backend.javaleap.model.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Admin issues a new API key for a telemetry gateway. */
public record ApiKeyCreateRequest(
        @NotBlank(message = "name is required") @Size(max = 80) String name,
        @Future(message = "expiresAt must be in the future") LocalDateTime expiresAt) {
}
