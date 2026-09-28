package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Daily generation for the station (sectorCode = null) or for one sector. */
public record DailyGenerationResponse(
        LocalDate genDate,
        String sectorCode,
        long steps,
        BigDecimal energyKwh,
        BigDecimal storedKwh,
        BigDecimal exportedKwh,
        BigDecimal peakPowerW) {
}
