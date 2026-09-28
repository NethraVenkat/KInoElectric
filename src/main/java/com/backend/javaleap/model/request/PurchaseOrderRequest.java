package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Create / edit a purchase order (replacement piezo tiles, transducer overhauls...). */
public record PurchaseOrderRequest(
        @NotNull(message = "vendorId is required") Long vendorId,
        @NotNull(message = "orderDate is required") LocalDate orderDate,
        LocalDate expectedDate,
        @Size(max = 30) String analyticAccountCode,
        @Size(max = 100) String reference,
        @Size(max = 500) String notes,
        @NotEmpty(message = "at least one line is required")
        @Size(max = 100) List<@Valid OrderLineRequest> lines) {
}
