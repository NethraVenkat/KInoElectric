package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Balance Sheet: Assets = Liabilities + Equity (equity includes current earnings). */
public record BalanceSheetResponse(
        LocalDate asOf,
        StatementSection assets,
        StatementSection liabilities,
        StatementSection equity,
        BigDecimal currentEarnings,
        BigDecimal totalAssets,
        BigDecimal totalLiabilitiesAndEquity,
        boolean balanced) {
}
