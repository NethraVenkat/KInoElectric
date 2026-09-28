package com.backend.javaleap.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Reverse a posted entry (reversalDate defaults to today). */
public record ReverseEntryRequest(
        LocalDate reversalDate,
        @NotBlank(message = "reason is required") @Size(max = 300) String reason) {
}
