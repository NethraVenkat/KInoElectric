package com.backend.javaleap.repository.projection;

import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;

import java.math.BigDecimal;

/** Posted debit/credit totals of one account (trial balance, financial statements). */
public interface AccountBalanceView {
    Long getAccountId();
    String getCode();
    String getName();
    AccountType getAccountType();
    AccountSubType getSubType();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
