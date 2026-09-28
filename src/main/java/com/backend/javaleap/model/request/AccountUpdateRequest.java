package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.AccountSubType;
import jakarta.validation.constraints.Size;

/** Partial account update - null fields unchanged. Sub-type is locked once the account has postings. */
public record AccountUpdateRequest(
        @Size(max = 120) String name,
        AccountSubType subType,
        Boolean reconcilable,
        Boolean active,
        @Size(max = 255) String description) {
}
