package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.ZoneType;

import java.math.BigDecimal;

/** Live aggregates for one zone ("energy by zone" chart, zone cards). */
public record ZoneStatsResponse(
        Long zoneId,
        String code,
        String name,
        ZoneType zoneType,
        long tileCount,
        long activeTiles,
        long faultyTiles,
        long stepsToday,
        BigDecimal energyTodayJ,
        BigDecimal energyTodayWh,
        BigDecimal avgHealthPct) {
}
