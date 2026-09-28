package com.backend.javaleap.entity.enums;

/** Life cycle of a zone budget. Only CONFIRMED budgets are used for monitoring. */
public enum BudgetStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED
}
