package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.BatteryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Result of processing a telemetry batch: energy harvested, where it was routed, alerts. */
public record TelemetryResultResponse(
        Long batchId,
        boolean duplicate,
        String stationCode,
        LocalDateTime recordedAt,
        int intervalSeconds,
        long totalSteps,
        BigDecimal energyJ,
        BigDecimal energyWh,
        BigDecimal energyKwh,
        BigDecimal averagePowerW,
        BigDecimal storedWh,
        BigDecimal exportedWh,
        BigDecimal batteryPct,
        BatteryStatus batteryStatus,
        int alertsRaised,
        List<SectorTelemetryResult> sectors) {
}
