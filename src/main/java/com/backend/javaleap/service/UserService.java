package com.backend.javaleap.service;

import com.backend.javaleap.entity.User;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.PasswordResetRequest;
import com.backend.javaleap.model.request.UserCreateRequest;
import com.backend.javaleap.model.request.UserUpdateRequest;
import com.backend.javaleap.model.response.UserResponse;
import com.backend.javaleap.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

/** User management (admin only - enforced in SecurityConfig). */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userMapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        String username = req.username().trim().toLowerCase(Locale.ROOT);
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '" + email + "' is already registered");
        }
        User saved = userRepository.save(userMapper.toEntity(req, passwordEncoder.encode(req.password())));
        log.info("Created user '{}' with role {}", saved.getUsername(), saved.getRole());
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest req) {
        User user = find(id);
        if (req.email() != null) {
            String email = req.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ConflictException("Email '" + email + "' is already registered");
            }
        }
        boolean self = Objects.equals(SecurityUtils.currentUserId().orElse(null), id);
        if (self && Boolean.FALSE.equals(req.enabled())) {
            throw new BadRequestException("You cannot disable your own account");
        }
        if (self && req.role() != null && req.role() != user.getRole()) {
            throw new BadRequestException("You cannot change your own role");
        }
        userMapper.applyUpdate(user, req);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest req) {
        User user = find(id);
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        log.info("Password reset for user '{}'", user.getUsername());
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
