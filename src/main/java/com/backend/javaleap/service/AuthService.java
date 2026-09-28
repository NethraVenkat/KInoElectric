package com.backend.javaleap.service;

import com.backend.javaleap.entity.User;
import com.backend.javaleap.exception.AccountLockedException;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.UserPrincipal;
import com.backend.javaleap.model.request.ChangePasswordRequest;
import com.backend.javaleap.model.request.LoginRequest;
import com.backend.javaleap.model.response.LoginResponse;
import com.backend.javaleap.model.response.UserResponse;
import com.backend.javaleap.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/** Login, current user and password change. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();

        if (loginAttemptService.isLocked(username)) {
            throw new AccountLockedException("Too many failed attempts. Try again in "
                    + LoginAttemptService.LOCK_DURATION.toMinutes() + " minutes.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(username);
            int left = loginAttemptService.remainingAttempts(username);
            log.warn("Failed login for '{}' ({} attempts left)", username, left);
            throw new BadCredentialsException(left > 0
                    ? "Invalid username or password. " + left + " attempt(s) left."
                    : "Invalid username or password. Account locked for "
                      + LoginAttemptService.LOCK_DURATION.toMinutes() + " minutes.");
        }

        loginAttemptService.loginSucceeded(username);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        user.setLastLoginAt(LocalDateTime.now());

        String token = jwtService.generateToken(principal);
        log.info("User '{}' ({}) logged in", principal.getUsername(), principal.getRole());

        return new LoginResponse(token, "Bearer", jwtService.getExpirationMs(),
                Instant.now().plusMillis(jwtService.getExpirationMs()), userMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return userMapper.toResponse(loadCurrentUser());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = loadCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("User '{}' changed password", user.getUsername());
    }

    private User loadCurrentUser() {
        Long id = SecurityUtils.currentUserId()
                .orElseThrow(() -> new BadCredentialsException("Not logged in"));
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
