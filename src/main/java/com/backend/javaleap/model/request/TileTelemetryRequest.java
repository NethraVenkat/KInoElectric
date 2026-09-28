package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Reading from a single tile: steps, and optionally its self-diagnosed health or a fault flag. */
public record TileTelemetryRequest(
        @NotBlank(message = "tileCode is required") @Size(max = 30) String tileCode,
        @Min(0) @Max(100_000) Integer steps,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal healthPct,
        Boolean fault) {
}
