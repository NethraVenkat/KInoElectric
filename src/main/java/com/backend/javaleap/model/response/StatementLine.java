package com.backend.javaleap.model.response;

import java.math.BigDecimal;

/** One account line on a financial statement. accountCode is null for computed lines. */
public record StatementLine(String accountCode, String accountName, BigDecimal amount) {
}
