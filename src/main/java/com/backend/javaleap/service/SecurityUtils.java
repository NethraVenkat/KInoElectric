package com.backend.javaleap.service;

import com.backend.javaleap.model.UserPrincipal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Helpers to read the logged-in user anywhere in the service layer. */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static Optional<UserPrincipal> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(UserPrincipal::getId);
    }
}
