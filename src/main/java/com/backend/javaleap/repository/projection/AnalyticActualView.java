package com.backend.javaleap.repository.projection;

import java.math.BigDecimal;

/** Posted debit/credit totals of one account inside one analytic account (budget actuals). */
public interface AnalyticActualView {
    Long getAnalyticAccountId();
    Long getAccountId();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
