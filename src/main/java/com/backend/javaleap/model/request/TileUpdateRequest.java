package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.TileType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Edit tile hardware details (admin / engineer). */
public record TileUpdateRequest(
        @NotNull(message = "tileType is required") TileType tileType,
        @NotNull(message = "ratedJoulesPerStep is required")
        @DecimalMin(value = "0.5", message = "ratedJoulesPerStep must be at least 0.5 J")
        @DecimalMax(value = "10.0", message = "ratedJoulesPerStep must be at most 10 J")
        BigDecimal ratedJoulesPerStep,
        @PastOrPresent(message = "lastMaintenanceOn cannot be in the future") LocalDate lastMaintenanceOn) {
}
