package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lightweight tile view - one per square on the floor map / row in the tile table. */
public record TileResponse(
        Long id,
        String tileCode,
        Long zoneId,
        String zoneCode,
        TileType tileType,
        TileStatus status,
        Integer gridRow,
        Integer gridCol,
        BigDecimal posX,
        BigDecimal posY,
        BigDecimal healthPct,
        Long stepsToday,
        BigDecimal energyTodayJ,
        LocalDateTime lastStepAt) {
}
