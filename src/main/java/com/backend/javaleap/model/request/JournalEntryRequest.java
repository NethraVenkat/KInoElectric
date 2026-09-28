package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Manual journal entry (draft). Total debit must equal total credit.
 * <pre>
 * { "journalCode": "MISC", "entryDate": "2026-09-27", "reference": "SUBSIDY-Q2",
 *   "narration": "Green energy subsidy received",
 *   "lines": [ { "accountCode": "1500", "debit": 25000 },
 *              { "accountCode": "4200", "credit": 25000 } ] }
 * </pre>
 */
public record JournalEntryRequest(
        @NotBlank(message = "journalCode is required") @Size(max = 10) String journalCode,
        @NotNull(message = "entryDate is required") LocalDate entryDate,
        @Size(max = 100) String reference,
        @Size(max = 500) String narration,
        @NotNull(message = "lines are required")
        @Size(min = 2, max = 200, message = "an entry needs 2-200 lines")
        List<@Valid JournalLineRequest> lines) {
}
