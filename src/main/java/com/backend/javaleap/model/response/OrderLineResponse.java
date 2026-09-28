package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.UnitOfMeasure;

import java.math.BigDecimal;

/** Line of a purchase or sales order. */
public record OrderLineResponse(
        Integer lineNo,
        String productCode,
        String productName,
        String description,
        String tileCode,
        String analyticAccountCode,
        BigDecimal quantity,
        UnitOfMeasure uom,
        BigDecimal unitPrice,
        BigDecimal gstRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total) {
}
