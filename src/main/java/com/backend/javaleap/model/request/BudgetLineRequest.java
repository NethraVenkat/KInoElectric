package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Planned amount for one zone (analytic account code) and one income/expense account code. */
public record BudgetLineRequest(
        @NotBlank(message = "analyticAccountCode is required") String analyticAccountCode,
        @NotBlank(message = "accountCode is required") String accountCode,
        @NotNull(message = "plannedAmount is required")
        @DecimalMin(value = "0.00", message = "plannedAmount cannot be negative") BigDecimal plannedAmount) {
}
