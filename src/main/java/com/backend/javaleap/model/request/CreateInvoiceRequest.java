package com.backend.javaleap.model.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Turn a confirmed order into a draft invoice / bill. */
public record CreateInvoiceRequest(
        LocalDate invoiceDate,
        @Size(max = 60) String vendorReference) {
}
