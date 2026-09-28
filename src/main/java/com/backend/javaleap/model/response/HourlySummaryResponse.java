package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One hour of rolled-up history (analytics charts). */
public record HourlySummaryResponse(
        LocalDateTime hourStart,
        Long steps,
        BigDecimal energyJ,
        BigDecimal energyWh,
        BigDecimal avgPowerW,
        BigDecimal peakPowerW,
        Integer footfall) {
}
