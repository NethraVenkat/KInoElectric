package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.PasswordResetRequest;
import com.backend.javaleap.model.request.UserCreateRequest;
import com.backend.javaleap.model.request.UserUpdateRequest;
import com.backend.javaleap.model.response.UserResponse;
import com.backend.javaleap.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** User management - ADMIN only. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "Manage control-centre users (admin)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users (paged)")
    public ApiResponse<PageResponse<UserResponse>> list(
            @PageableDefault(size = 20, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(userService.list(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok("User created", userService.create(request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update email, name, role or enabled flag")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Set a new password for a user")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.ok("Password reset", null);
    }
}
