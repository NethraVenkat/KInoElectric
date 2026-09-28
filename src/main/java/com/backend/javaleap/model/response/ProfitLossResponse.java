package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Kinetic Energy Profit & Loss for a period. */
public record ProfitLossResponse(
        LocalDate from,
        LocalDate to,
        StatementSection income,
        StatementSection expenses,
        BigDecimal netProfit,
        BigDecimal marginPct) {
}
