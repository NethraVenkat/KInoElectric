package com.backend.javaleap.model.response;

import java.math.BigDecimal;

/** Invoice / bill line with its GL account. */
public record InvoiceLineResponse(
        Integer lineNo,
        String productCode,
        String description,
        String accountCode,
        String accountName,
        String analyticAccountCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal gstRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total) {
}
