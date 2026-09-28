package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Register a payment against a posted invoice. Direction (receipt / payment) follows the invoice type.
 * journalCode defaults to BNK (or CSH for CASH).
 */
public record PaymentRequest(
        @NotNull(message = "invoiceId is required") Long invoiceId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Digits(integer = 14, fraction = 2) BigDecimal amount,
        LocalDate paymentDate,
        @NotNull(message = "method is required") PaymentMethod method,
        @Size(max = 10) String journalCode,
        @Size(max = 60) String reference,
        @Size(max = 255) String notes) {
}
