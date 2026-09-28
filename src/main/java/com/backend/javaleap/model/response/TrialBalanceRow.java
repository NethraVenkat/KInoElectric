package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.AccountType;

import java.math.BigDecimal;

/** One account in the trial balance: movement totals and the resulting debit or credit balance. */
public record TrialBalanceRow(
        String accountCode,
        String accountName,
        AccountType accountType,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal debitBalance,
        BigDecimal creditBalance) {
}
