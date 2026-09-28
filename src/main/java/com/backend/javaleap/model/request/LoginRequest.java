package com.backend.javaleap.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login with username (or email) and password. */
public record LoginRequest(
        @NotBlank(message = "username is required") @Size(max = 120) String username,
        @NotBlank(message = "password is required") @Size(max = 64) String password) {
}
