package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** General ledger of one account for a period. Balances are debit minus credit. */
public record LedgerResponse(
        String accountCode,
        String accountName,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal closingBalance,
        List<LedgerLineResponse> lines) {
}
