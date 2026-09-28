package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/kinetic/telemetry - step counts reported by a station gateway.
 * <pre>
 * {
 *   "stationCode": "CHC",
 *   "batchRef": "CHC-GW01-20260927-101500",   (optional, makes retries safe)
 *   "source": "GATEWAY-01",
 *   "recordedAt": "2026-09-27T10:15:00",       (optional, default now)
 *   "intervalSeconds": 60,
 *   "sectors": [
 *     { "sectorCode": "ENT", "steps": 420 },
 *     { "sectorCode": "PF1", "tiles": [ { "tileCode": "CHC-PF1-R1C01", "steps": 12 } ] }
 *   ]
 * }
 * </pre>
 */
public record TelemetryRequest(
        @NotBlank(message = "stationCode is required") @Size(max = 20) String stationCode,
        @Size(max = 64) String batchRef,
        @Size(max = 60) String source,
        LocalDateTime recordedAt,
        @Min(value = 1, message = "intervalSeconds must be 1-3600")
        @Max(value = 3600, message = "intervalSeconds must be 1-3600")
        Integer intervalSeconds,
        @NotEmpty(message = "at least one sector is required")
        @Size(max = 50, message = "at most 50 sectors per batch")
        List<@Valid SectorTelemetryRequest> sectors) {
}
