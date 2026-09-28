package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.AccountType;

import java.math.BigDecimal;

/**
 * Planned vs actual for one zone and account. variance is positive when favourable:
 * revenue above plan, or cost below plan.
 */
public record BudgetLineResponse(
        Long id,
        String analyticAccountCode,
        String analyticAccountName,
        String accountCode,
        String accountName,
        AccountType accountType,
        BigDecimal plannedAmount,
        BigDecimal actualAmount,
        BigDecimal variance,
        BigDecimal achievementPct) {
}
