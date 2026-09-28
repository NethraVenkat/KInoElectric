package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Create or fully update a product (kWh, kinetic tile, overhaul service). */
public record ProductRequest(
        @NotBlank(message = "code is required")
        @Pattern(regexp = "^[A-Z0-9-]{2,30}$", message = "code must be 2-30 uppercase letters, digits or '-'")
        String code,
        @NotBlank(message = "name is required") @Size(max = 150) String name,
        @NotNull(message = "productType is required") ProductType productType,
        @NotNull(message = "uom is required") UnitOfMeasure uom,
        @Pattern(regexp = "^$|^[0-9]{4,8}$", message = "hsnSac must be 4-8 digits") String hsnSac,
        @DecimalMin(value = "0.0000", message = "salePrice cannot be negative")
        @Digits(integer = 10, fraction = 4) BigDecimal salePrice,
        @DecimalMin(value = "0.0000", message = "purchasePrice cannot be negative")
        @Digits(integer = 10, fraction = 4) BigDecimal purchasePrice,
        @DecimalMin(value = "0.00", message = "gstRate must be 0-40")
        @DecimalMax(value = "40.00", message = "gstRate must be 0-40") BigDecimal gstRate,
        Boolean canBeSold,
        Boolean canBePurchased,
        @Size(max = 20) String incomeAccountCode,
        @Size(max = 20) String expenseAccountCode,
        @Size(max = 500) String description) {
}
