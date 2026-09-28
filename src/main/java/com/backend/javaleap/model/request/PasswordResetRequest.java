package com.backend.javaleap.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin sets a new password for another user. */
public record PasswordResetRequest(
        @NotBlank(message = "newPassword is required")
        @Size(min = 8, max = 64, message = "newPassword must be 8-64 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "newPassword must contain letters and digits")
        String newPassword) {
}
