package com.backend.javaleap.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Partial update of a sector - null fields are unchanged. */
public record SectorUpdateRequest(
        @Size(max = 80) String name,
        @DecimalMin("0.10") @DecimalMax("5.00") BigDecimal trafficWeight,
        @Min(10) @Max(10000) Integer maxCapacity) {
}
