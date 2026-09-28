package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Partial update of a user - null fields are left unchanged. */
public record UserUpdateRequest(
        @Email(message = "email must be valid") @Size(max = 120) String email,
        @Size(max = 100) String fullName,
        Role role,
        Boolean enabled) {
}
