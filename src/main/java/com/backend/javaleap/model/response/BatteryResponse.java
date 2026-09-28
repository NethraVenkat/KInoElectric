package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.BatteryStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Battery bank state incl. computed charge percentage. */
public record BatteryResponse(
        Long id,
        String code,
        String name,
        String chemistry,
        BigDecimal capacityWh,
        BigDecimal chargeWh,
        BigDecimal chargePct,
        BigDecimal maxChargeRateW,
        BigDecimal temperatureC,
        Integer cycleCount,
        BigDecimal healthPct,
        BatteryStatus status,
        LocalDateTime updatedAt) {
}
