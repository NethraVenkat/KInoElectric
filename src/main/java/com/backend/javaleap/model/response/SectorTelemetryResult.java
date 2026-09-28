package com.backend.javaleap.model.response;

import java.math.BigDecimal;

/** What one sector produced in a telemetry batch. */
public record SectorTelemetryResult(
        String sectorCode,
        long steps,
        long lostSteps,
        BigDecimal energyJ,
        BigDecimal energyWh,
        int occupancy,
        BigDecimal densityPct,
        int tilesUpdated) {
}
