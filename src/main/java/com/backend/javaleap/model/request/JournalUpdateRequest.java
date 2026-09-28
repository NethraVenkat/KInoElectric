package com.backend.javaleap.model.request;

import jakarta.validation.constraints.Size;

/** Admin edits a journal's name, default account or active flag. */
public record JournalUpdateRequest(
        @Size(max = 80) String name,
        @Size(max = 20) String defaultAccountCode,
        Boolean active) {
}
