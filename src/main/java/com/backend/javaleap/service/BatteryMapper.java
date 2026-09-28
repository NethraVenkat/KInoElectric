package com.backend.javaleap.service;

import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.model.response.BatteryResponse;

import org.springframework.stereotype.Component;

/** Battery entity -> DTO (adds computed charge %). */
@Component
public class BatteryMapper {

    public BatteryResponse toResponse(Battery b) {
        if (b == null) {
            return null;
        }
        return new BatteryResponse(b.getId(), b.getCode(), b.getName(), b.getChemistry(),
                b.getCapacityWh(), b.getChargeWh(), b.getChargePct(), b.getMaxChargeRateW(),
                b.getTemperatureC(), b.getCycleCount(), b.getHealthPct(), b.getStatus(), b.getUpdatedAt());
    }
}
