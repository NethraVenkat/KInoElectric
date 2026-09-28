package com.backend.javaleap.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Station-level generation totals for one day. */
public interface DailyGenerationView {
    LocalDate getGenDate();
    Long getSteps();
    BigDecimal getEnergyJ();
    BigDecimal getStoredJ();
    BigDecimal getExportedJ();
    BigDecimal getPeakPowerW();
}
