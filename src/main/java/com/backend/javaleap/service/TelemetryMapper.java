package com.backend.javaleap.service;

import com.backend.javaleap.entity.EnergySummaryHourly;
import com.backend.javaleap.entity.Reading;
import com.backend.javaleap.entity.ZoneReading;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.response.HourlySummaryResponse;
import com.backend.javaleap.model.response.ReadingResponse;
import com.backend.javaleap.model.response.ZoneReadingResponse;

import org.springframework.stereotype.Component;

/** Time-series entities (readings, zone readings, hourly summaries) -> DTO. */
@Component
public class TelemetryMapper {

    public ReadingResponse toResponse(Reading r) {
        if (r == null) {
            return null;
        }
        return new ReadingResponse(r.getRecordedAt(), r.getSteps(), r.getEnergyJ(), r.getPowerW(),
                r.getBatteryPct(), r.getActiveTiles());
    }

    public ZoneReadingResponse toResponse(ZoneReading r) {
        if (r == null) {
            return null;
        }
        return new ZoneReadingResponse(r.getZone().getId(), r.getZone().getCode(), r.getRecordedAt(),
                r.getSteps(), r.getEnergyJ(), r.getOccupancy(), r.getDensityPct());
    }

    public HourlySummaryResponse toResponse(EnergySummaryHourly h) {
        if (h == null) {
            return null;
        }
        return new HourlySummaryResponse(h.getHourStart(), h.getSteps(), h.getEnergyJ(),
                EnergyUnits.toWh(h.getEnergyJ()), h.getAvgPowerW(), h.getPeakPowerW(), h.getFootfall());
    }
}
