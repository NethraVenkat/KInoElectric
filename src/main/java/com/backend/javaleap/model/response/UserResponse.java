package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.Role;

import java.time.LocalDateTime;

/** Public user profile - never exposes the password hash. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        Boolean enabled,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {
}
