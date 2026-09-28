package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.EntryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Journal entry with lines (lines are empty in list views). */
public record JournalEntryResponse(
        Long id,
        String entryNumber,
        String journalCode,
        LocalDate entryDate,
        String reference,
        String narration,
        EntryStatus status,
        EntrySourceType sourceType,
        Long sourceId,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        Long reversalOfId,
        String reversalOfNumber,
        String createdBy,
        LocalDateTime postedAt,
        String postedBy,
        List<JournalLineResponse> lines) {
}
