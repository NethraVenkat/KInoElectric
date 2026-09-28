package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;

import java.math.BigDecimal;

/** Product master record. */
public record ProductResponse(
        Long id,
        String code,
        String name,
        ProductType productType,
        UnitOfMeasure uom,
        String hsnSac,
        BigDecimal salePrice,
        BigDecimal purchasePrice,
        BigDecimal gstRate,
        boolean canBeSold,
        boolean canBePurchased,
        String incomeAccountCode,
        String expenseAccountCode,
        String description,
        boolean active) {
}
