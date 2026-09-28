package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.TileType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Current conversion rate of a tile technology. */
public record ConversionRateResponse(
        TileType tileType,
        BigDecimal joulesPerStep,
        BigDecimal conversionEfficiency,
        BigDecimal effectiveJoulesPerStep,
        String description,
        String updatedBy,
        LocalDateTime updatedAt,
        long tilesOfThisType) {
}
