package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the telemetry audit log. */
public record TelemetryBatchResponse(
        Long id,
        String batchRef,
        String source,
        LocalDateTime recordedAt,
        LocalDateTime receivedAt,
        Integer intervalSeconds,
        Integer sectorCount,
        Long steps,
        BigDecimal energyWh,
        BigDecimal storedWh,
        BigDecimal exportedWh,
        String submittedBy) {
}
