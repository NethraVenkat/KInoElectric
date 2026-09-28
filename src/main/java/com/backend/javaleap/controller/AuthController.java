package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.ChangePasswordRequest;
import com.backend.javaleap.model.request.LoginRequest;
import com.backend.javaleap.model.response.LoginResponse;
import com.backend.javaleap.model.response.UserResponse;
import com.backend.javaleap.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication endpoints. Logout is client-side: simply discard the token. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login and current user")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in and receive a JWT access token")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Profile of the logged-in user")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change your own password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok("Password changed", null);
    }
}
