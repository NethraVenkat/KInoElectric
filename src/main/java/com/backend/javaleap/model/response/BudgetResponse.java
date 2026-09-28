package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Budget with totals (Budget Report): planned vs actual revenue, cost and net result. */
public record BudgetResponse(
        Long id,
        String code,
        String name,
        LocalDate dateFrom,
        LocalDate dateTo,
        BudgetStatus status,
        String notes,
        BigDecimal plannedRevenue,
        BigDecimal actualRevenue,
        BigDecimal plannedCost,
        BigDecimal actualCost,
        BigDecimal plannedNet,
        BigDecimal actualNet,
        List<BudgetLineResponse> lines) {
}
