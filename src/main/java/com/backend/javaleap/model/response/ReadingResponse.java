package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One point on the live power chart. */
public record ReadingResponse(
        LocalDateTime recordedAt,
        Integer steps,
        BigDecimal energyJ,
        BigDecimal powerW,
        BigDecimal batteryPct,
        Integer activeTiles) {
}
