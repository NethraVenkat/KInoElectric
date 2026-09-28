package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.response.SystemSummaryResponse;
import com.backend.javaleap.service.SystemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** System / data-layer information. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Backend status")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/summary")
    @Operation(summary = "Counts of stations, zones, tiles, batteries, users, alerts in the database")
    public ApiResponse<SystemSummaryResponse> summary() {
        return ApiResponse.ok(systemService.getSummary());
    }
}
