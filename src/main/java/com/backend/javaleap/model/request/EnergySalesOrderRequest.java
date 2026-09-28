package com.backend.javaleap.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Generate the energy export sales order for a billing period from telemetry:
 * one line per floor sector = kWh exported x tariff, tagged with the sector's analytic account.
 */
public record EnergySalesOrderRequest(
        @NotNull(message = "customerId is required") Long customerId,
        @Size(max = 20) String stationCode,
        @NotNull(message = "periodStart is required") LocalDate periodStart,
        @NotNull(message = "periodEnd is required") LocalDate periodEnd,
        @Size(max = 30) String productCode) {
}
