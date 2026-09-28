package com.backend.javaleap.model.response;

import java.math.BigDecimal;

/** Snapshot of what is stored in the database - used to verify the data layer. */
public record SystemSummaryResponse(
        String stationCode,
        String stationName,
        long zones,
        long tiles,
        long activeTiles,
        long faultyTiles,
        long maintenanceTiles,
        BigDecimal avgTileHealthPct,
        long batteries,
        BigDecimal batteryChargePct,
        long users,
        long openAlerts,
        long hourlyHistoryRows) {
}
