package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create a new control-centre user (admin only). */
public record UserCreateRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 50, message = "username must be 3-50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username may contain letters, digits, . _ - only")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 120)
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 64, message = "password must be 8-64 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "password must contain letters and digits")
        String password,

        @NotBlank(message = "fullName is required")
        @Size(max = 100)
        String fullName,

        @NotNull(message = "role is required")
        Role role) {
}
