package com.backend.javaleap.model.response;

import java.math.BigDecimal;

/** One line of a journal entry. */
public record JournalLineResponse(
        Integer lineNo,
        String accountCode,
        String accountName,
        Long contactId,
        String contactName,
        String analyticAccountCode,
        String label,
        BigDecimal debit,
        BigDecimal credit) {
}
