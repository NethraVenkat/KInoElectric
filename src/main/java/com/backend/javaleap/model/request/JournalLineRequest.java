package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One journal line: exactly one of debit / credit must be greater than zero. */
public record JournalLineRequest(
        @NotBlank(message = "accountCode is required") @Size(max = 20) String accountCode,
        Long contactId,
        @Size(max = 30) String analyticAccountCode,
        @Size(max = 255) String label,
        @DecimalMin(value = "0.00", message = "debit cannot be negative") @Digits(integer = 14, fraction = 2)
        BigDecimal debit,
        @DecimalMin(value = "0.00", message = "credit cannot be negative") @Digits(integer = 14, fraction = 2)
        BigDecimal credit) {
}
