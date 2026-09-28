package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Steps for one sector. Either send {@code steps} (spread over the sector's working tiles)
 * or per-tile readings in {@code tiles} (more precise). Both may be combined.
 */
public record SectorTelemetryRequest(
        @NotBlank(message = "sectorCode is required") @Size(max = 10) String sectorCode,
        @Min(value = 0, message = "steps cannot be negative")
        @Max(value = 5_000_000, message = "steps too large for one interval")
        Long steps,
        @Size(max = 500) List<@Valid TileTelemetryRequest> tiles) {
}
