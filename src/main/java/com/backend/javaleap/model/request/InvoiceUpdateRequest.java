package com.backend.javaleap.model.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Edit header fields of a draft invoice. */
public record InvoiceUpdateRequest(
        LocalDate invoiceDate,
        LocalDate dueDate,
        @Size(max = 60) String vendorReference,
        @Size(max = 500) String notes) {
}
