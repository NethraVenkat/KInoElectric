package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Generation report for a period: totals + daily rows. */
public record GenerationSummaryResponse(
        String stationCode,
        LocalDate from,
        LocalDate to,
        long totalSteps,
        BigDecimal totalEnergyKwh,
        BigDecimal totalStoredKwh,
        BigDecimal totalExportedKwh,
        List<DailyGenerationResponse> days) {
}
