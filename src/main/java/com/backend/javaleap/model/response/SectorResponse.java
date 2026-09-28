package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.ZoneType;

import java.math.BigDecimal;

/** Floor tile sector with live totals and its analytic (budget) account. */
public record SectorResponse(
        Long id,
        String stationCode,
        String code,
        String name,
        ZoneType zoneType,
        BigDecimal trafficWeight,
        Integer maxCapacity,
        Integer posX,
        Integer posY,
        Integer width,
        Integer height,
        Integer gridRows,
        Integer gridCols,
        long tileCount,
        long activeTiles,
        long faultyTiles,
        long stepsToday,
        BigDecimal energyTodayWh,
        BigDecimal avgHealthPct,
        String analyticAccountCode,
        String analyticAccountName) {
}
