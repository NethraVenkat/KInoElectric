package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Transit Energy Director updates the kinetic conversion rate of a tile technology. */
public record ConversionRateUpdateRequest(
        @NotNull(message = "joulesPerStep is required")
        @DecimalMin(value = "0.10", message = "joulesPerStep must be at least 0.10 J")
        @DecimalMax(value = "10.00", message = "joulesPerStep must be at most 10 J")
        BigDecimal joulesPerStep,

        @NotNull(message = "conversionEfficiency is required")
        @DecimalMin(value = "0.010", message = "conversionEfficiency must be between 0.01 and 1")
        @DecimalMax(value = "1.000", message = "conversionEfficiency must be between 0.01 and 1")
        BigDecimal conversionEfficiency,

        @Size(max = 255) String description,

        /** Also update the rated J/step of every existing tile of this type. */
        Boolean applyToExistingTiles) {
}
