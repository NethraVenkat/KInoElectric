package com.backend.javaleap.repository.projection;

import com.backend.javaleap.entity.enums.ZoneType;

import java.math.BigDecimal;

/** Aggregated live statistics for one zone (tiles, steps, energy, health). */
public interface ZoneStatsView {
    Long getZoneId();
    String getCode();
    String getName();
    ZoneType getZoneType();
    Long getTileCount();
    Long getActiveTiles();
    Long getFaultyTiles();
    Long getStepsToday();
    BigDecimal getEnergyTodayJ();
    Double getAvgHealthPct();
}
