package com.backend.javaleap.service;

import com.backend.javaleap.entity.User;
import com.backend.javaleap.model.request.UserCreateRequest;
import com.backend.javaleap.model.request.UserUpdateRequest;
import com.backend.javaleap.model.response.UserResponse;

import org.springframework.stereotype.Component;

import java.util.Locale;

/** User entity <-> DTO. The password is hashed by the caller, never here. */
@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        if (u == null) {
            return null;
        }
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole(),
                u.getEnabled(), u.getLastLoginAt(), u.getCreatedAt());
    }

    public User toEntity(UserCreateRequest req, String passwordHash) {
        return User.builder()
                .username(req.username().trim().toLowerCase(Locale.ROOT))
                .email(req.email().trim().toLowerCase(Locale.ROOT))
                .passwordHash(passwordHash)
                .fullName(req.fullName().trim())
                .role(req.role())
                .enabled(Boolean.TRUE)
                .build();
    }

    /** Applies only the non-null fields of the request. */
    public void applyUpdate(User u, UserUpdateRequest req) {
        if (req.email() != null) {
            u.setEmail(req.email().trim().toLowerCase(Locale.ROOT));
        }
        if (req.fullName() != null) {
            u.setFullName(req.fullName().trim());
        }
        if (req.role() != null) {
            u.setRole(req.role());
        }
        if (req.enabled() != null) {
            u.setEnabled(req.enabled());
        }
    }
}
