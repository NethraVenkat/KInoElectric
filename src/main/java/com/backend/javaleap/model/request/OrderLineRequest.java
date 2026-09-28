package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Order line. unitPrice and gstRate default from the product when omitted.
 * tileCode (purchase orders only) links the line to the tile being repaired.
 */
public record OrderLineRequest(
        @NotBlank(message = "productCode is required") @Size(max = 30) String productCode,
        @Size(max = 255) String description,
        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.001", message = "quantity must be greater than 0")
        @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @DecimalMin(value = "0.0000", message = "unitPrice cannot be negative")
        @Digits(integer = 10, fraction = 4) BigDecimal unitPrice,
        @DecimalMin(value = "0.00", message = "gstRate must be 0-40")
        @DecimalMax(value = "40.00", message = "gstRate must be 0-40") BigDecimal gstRate,
        @Size(max = 30) String analyticAccountCode,
        @Size(max = 30) String tileCode) {
}
