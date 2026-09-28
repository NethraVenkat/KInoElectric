package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One posting in an account ledger with the running balance (debit minus credit). */
public record LedgerLineResponse(
        LocalDate date,
        Long entryId,
        String entryNumber,
        String journalCode,
        String reference,
        String label,
        String contactName,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal runningBalance) {
}
