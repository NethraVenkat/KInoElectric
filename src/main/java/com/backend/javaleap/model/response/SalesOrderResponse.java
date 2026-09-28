package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Sales order with lines and the linked customer invoice. */
public record SalesOrderResponse(
        Long id,
        String soNumber,
        OrderStatus status,
        Long customerId,
        String customerCode,
        String customerName,
        LocalDate orderDate,
        String stationCode,
        LocalDate periodStart,
        LocalDate periodEnd,
        String reference,
        String notes,
        BigDecimal untaxedAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        Long invoiceId,
        String invoiceNumber,
        LocalDateTime confirmedAt,
        String createdBy,
        List<OrderLineResponse> lines) {
}
