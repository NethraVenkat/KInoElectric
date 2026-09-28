package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Simple liveness endpoint to confirm the backend is running. */
@RestController
@RequestMapping(AppConstants.API_BASE)
@RequiredArgsConstructor
@Tag(name = "System", description = "Backend status")
public class HealthController {

    private final Environment environment;

    @GetMapping("/health")
    @Operation(summary = "Check backend status")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", AppConstants.APP_NAME + " backend");
        info.put("status", "RUNNING");
        info.put("version", AppConstants.APP_VERSION);
        info.put("profiles", Arrays.asList(environment.getActiveProfiles()));
        info.put("serverTime", Instant.now());
        return ApiResponse.ok("PadaShakti backend running", info);
    }
}
