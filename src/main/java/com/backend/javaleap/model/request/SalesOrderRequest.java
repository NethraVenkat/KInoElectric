package com.backend.javaleap.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Create / edit a manual sales order. For monthly energy export use EnergySalesOrderRequest. */
public record SalesOrderRequest(
        @NotNull(message = "customerId is required") Long customerId,
        @NotNull(message = "orderDate is required") LocalDate orderDate,
        @Size(max = 100) String reference,
        @Size(max = 500) String notes,
        @NotEmpty(message = "at least one line is required")
        @Size(max = 100) List<@Valid OrderLineRequest> lines) {
}
