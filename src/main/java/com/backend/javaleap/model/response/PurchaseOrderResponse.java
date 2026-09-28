package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Purchase order with lines and the linked vendor bill. */
public record PurchaseOrderResponse(
        Long id,
        String poNumber,
        OrderStatus status,
        Long vendorId,
        String vendorCode,
        String vendorName,
        LocalDate orderDate,
        LocalDate expectedDate,
        String analyticAccountCode,
        String reference,
        String notes,
        BigDecimal untaxedAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long billId,
        String billNumber,
        LocalDateTime confirmedAt,
        String createdBy,
        List<OrderLineResponse> lines) {
}
