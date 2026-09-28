package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.InvoiceType;
import com.backend.javaleap.entity.enums.PaymentState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Customer invoice / vendor bill with GST split, payments and its journal entry. */
public record InvoiceResponse(
        Long id,
        InvoiceType invoiceType,
        String invoiceNumber,
        InvoiceStatus status,
        PaymentState paymentState,
        Long contactId,
        String contactCode,
        String contactName,
        String contactGstin,
        LocalDate invoiceDate,
        LocalDate dueDate,
        long overdueDays,
        String sourceOrderNumber,
        String vendorReference,
        String placeOfSupply,
        BigDecimal untaxedAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        Long journalEntryId,
        String notes,
        LocalDateTime postedAt,
        List<InvoiceLineResponse> lines,
        List<PaymentResponse> payments) {
}
