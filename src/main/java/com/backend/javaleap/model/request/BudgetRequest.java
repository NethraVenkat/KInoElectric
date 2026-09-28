package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Create a zone budget for a period. */
public record BudgetRequest(
        @NotBlank(message = "code is required") @Size(max = 30) String code,
        @NotBlank(message = "name is required") @Size(max = 150) String name,
        @NotNull(message = "dateFrom is required") LocalDate dateFrom,
        @NotNull(message = "dateTo is required") LocalDate dateTo,
        @Size(max = 500) String notes,
        @NotEmpty(message = "at least one budget line is required") List<@Valid BudgetLineRequest> lines) {
}
