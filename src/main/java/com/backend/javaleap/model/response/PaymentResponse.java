package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.PaymentMethod;
import com.backend.javaleap.entity.enums.PaymentStatus;
import com.backend.javaleap.entity.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Customer receipt or vendor payment. */
public record PaymentResponse(
        Long id,
        String paymentNumber,
        PaymentType paymentType,
        PaymentStatus status,
        Long contactId,
        String contactName,
        Long invoiceId,
        String invoiceNumber,
        LocalDate paymentDate,
        BigDecimal amount,
        PaymentMethod method,
        String journalCode,
        String reference,
        Long journalEntryId,
        String notes,
        LocalDateTime createdAt) {
}
