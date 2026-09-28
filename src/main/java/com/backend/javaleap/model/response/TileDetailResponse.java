package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Full tile details for the side panel when a tile is clicked. */
public record TileDetailResponse(
        Long id,
        String tileCode,
        Long zoneId,
        String zoneCode,
        String zoneName,
        TileType tileType,
        TileStatus status,
        Integer gridRow,
        Integer gridCol,
        BigDecimal posX,
        BigDecimal posY,
        BigDecimal ratedJoulesPerStep,
        BigDecimal healthPct,
        Long stepsToday,
        BigDecimal energyTodayJ,
        BigDecimal energyTodayWh,
        Long totalSteps,
        BigDecimal totalEnergyJ,
        BigDecimal totalEnergyWh,
        LocalDate installedOn,
        LocalDate lastMaintenanceOn,
        LocalDateTime lastStepAt,
        LocalDateTime updatedAt) {
}
