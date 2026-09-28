package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Contact;

import java.math.BigDecimal;

/**
 * A resolved journal line used by system postings (invoices, bills, payments in Phase D).
 */
public record PostingLine(Account account, Contact contact, AnalyticAccount analyticAccount, String label,
                          BigDecimal debit, BigDecimal credit) {

    public static PostingLine debit(Account account, BigDecimal amount, String label) {
        return new PostingLine(account, null, null, label, amount, BigDecimal.ZERO);
    }

    public static PostingLine credit(Account account, BigDecimal amount, String label) {
        return new PostingLine(account, null, null, label, BigDecimal.ZERO, amount);
    }

    public PostingLine withContact(Contact c) {
        return new PostingLine(account, c, analyticAccount, label, debit, credit);
    }

    public PostingLine withAnalytic(AnalyticAccount a) {
        return new PostingLine(account, contact, a, label, debit, credit);
    }
}
