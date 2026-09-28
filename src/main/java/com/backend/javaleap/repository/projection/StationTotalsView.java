package com.backend.javaleap.repository.projection;

import java.math.BigDecimal;

/** Station-wide totals across all tiles. */
public interface StationTotalsView {
    Long getTotalTiles();
    Long getActiveTiles();
    Long getFaultyTiles();
    Long getMaintenanceTiles();
    Long getStepsToday();
    BigDecimal getEnergyTodayJ();
    Long getTotalSteps();
    BigDecimal getTotalEnergyJ();
    Double getAvgHealthPct();
}
