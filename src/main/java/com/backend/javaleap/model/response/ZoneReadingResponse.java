package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One per-zone snapshot (crowd density & energy). */
public record ZoneReadingResponse(
        Long zoneId,
        String zoneCode,
        LocalDateTime recordedAt,
        Integer steps,
        BigDecimal energyJ,
        Integer occupancy,
        BigDecimal densityPct) {
}
