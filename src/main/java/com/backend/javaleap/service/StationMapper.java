package com.backend.javaleap.service;

import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.response.StationResponse;
import com.backend.javaleap.model.response.ZoneResponse;
import com.backend.javaleap.model.response.ZoneStatsResponse;
import com.backend.javaleap.repository.projection.ZoneStatsView;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Station and Zone entity/projection -> DTO. */
@Component
public class StationMapper {

    public StationResponse toResponse(Station s) {
        if (s == null) {
            return null;
        }
        return new StationResponse(s.getId(), s.getCode(), s.getName(), s.getCity(), s.getLineName(),
                s.getLatitude(), s.getLongitude(), s.getDailyFootfallEstimate(),
                s.getLayoutWidth(), s.getLayoutHeight(), s.getStatus());
    }

    public ZoneResponse toResponse(Zone z) {
        if (z == null) {
            return null;
        }
        return new ZoneResponse(z.getId(), z.getCode(), z.getName(), z.getZoneType(), z.getTrafficWeight(),
                z.getMaxCapacity(), z.getPosX(), z.getPosY(), z.getWidth(), z.getHeight(),
                z.getGridRows(), z.getGridCols(), z.getDisplayOrder());
    }

    public ZoneStatsResponse toResponse(ZoneStatsView v) {
        if (v == null) {
            return null;
        }
        BigDecimal energy = v.getEnergyTodayJ() == null ? BigDecimal.ZERO : v.getEnergyTodayJ();
        return new ZoneStatsResponse(v.getZoneId(), v.getCode(), v.getName(), v.getZoneType(),
                nz(v.getTileCount()), nz(v.getActiveTiles()), nz(v.getFaultyTiles()), nz(v.getStepsToday()),
                energy, EnergyUnits.toWh(energy), EnergyUnits.round2(v.getAvgHealthPct()));
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }
}
