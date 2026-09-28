package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.JournalType;

/** Journal with its numbering preview. */
public record JournalResponse(
        Long id,
        String code,
        String name,
        JournalType journalType,
        String defaultAccountCode,
        String defaultAccountName,
        String sequencePrefix,
        String nextNumber,
        boolean active) {
}
