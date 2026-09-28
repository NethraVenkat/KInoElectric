package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.ApiKeyCreateRequest;
import com.backend.javaleap.model.response.ApiKeyCreatedResponse;
import com.backend.javaleap.model.response.ApiKeyResponse;
import com.backend.javaleap.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API keys for telemetry gateways (System actor) - ADMIN only. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - API keys", description = "Machine credentials for the System actor")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @Operation(summary = "List API keys")
    public ApiResponse<List<ApiKeyResponse>> list() {
        return ApiResponse.ok(apiKeyService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue a new key (shown once)")
    public ApiResponse<ApiKeyCreatedResponse> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return ApiResponse.ok("API key created", apiKeyService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke a key")
    public ApiResponse<ApiKeyResponse> revoke(@PathVariable Long id) {
        return ApiResponse.ok("API key revoked", apiKeyService.revoke(id));
    }
}
