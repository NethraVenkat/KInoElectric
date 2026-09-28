package com.backend.javaleap.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Simulation speed multiplier (1 = real time, 60 = one minute per second). */
public record SimulationSpeedRequest(
        @NotNull(message = "speed is required")
        @Min(value = 1, message = "speed must be 1-120")
        @Max(value = 120, message = "speed must be 1-120")
        Integer speed) {
}
