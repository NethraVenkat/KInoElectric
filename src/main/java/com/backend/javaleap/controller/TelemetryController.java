package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.TelemetryRequest;
import com.backend.javaleap.model.response.GenerationSummaryResponse;
import com.backend.javaleap.model.response.TelemetryBatchResponse;
import com.backend.javaleap.model.response.TelemetryResultResponse;
import com.backend.javaleap.service.SecurityUtils;
import com.backend.javaleap.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Telemetry ingestion (System actor) and generation logs. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/kinetic")
@RequiredArgsConstructor
@Tag(name = "Kinetic - Telemetry", description = "Step/kWh ingestion and daily generation")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/telemetry")
    @PreAuthorize("hasAnyRole('SYSTEM', 'ADMIN')")
    @Operation(summary = "Ingest step telemetry (X-API-KEY or SYSTEM/ADMIN token)")
    public ApiResponse<TelemetryResultResponse> ingest(@Valid @RequestBody TelemetryRequest request) {
        TelemetryResultResponse result = telemetryService.ingest(request, SecurityUtils.currentUserId().orElse(null));
        return ApiResponse.ok(result.duplicate() ? "Duplicate batch - already processed" : "Telemetry processed",
                result);
    }

    @GetMapping("/telemetry/recent")
    @Operation(summary = "Last 50 telemetry batches (audit log)")
    public ApiResponse<List<TelemetryBatchResponse>> recent(@RequestParam(required = false) String stationCode) {
        return ApiResponse.ok(telemetryService.recentBatches(stationCode));
    }

    @GetMapping("/generation/daily")
    @Operation(summary = "Daily generation log (station totals or per sector)")
    public ApiResponse<GenerationSummaryResponse> daily(
            @RequestParam(required = false) String stationCode,
            @Parameter(description = "yyyy-MM-dd, default 30 days ago")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "yyyy-MM-dd, default today")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean bySector) {
        return ApiResponse.ok(telemetryService.generation(stationCode, from, to, bySector));
    }
}
