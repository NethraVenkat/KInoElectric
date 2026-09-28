package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Trial balance - proves the books balance (sum of debit balances = sum of credit balances). */
public record TrialBalanceResponse(
        LocalDate asOf,
        List<TrialBalanceRow> rows,
        BigDecimal totalDebitBalance,
        BigDecimal totalCreditBalance,
        boolean balanced) {
}
