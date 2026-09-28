package com.backend.javaleap.service;

import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.response.TileDetailResponse;
import com.backend.javaleap.model.response.TileResponse;

import org.springframework.stereotype.Component;

/**
 * Tile entity -> DTO.
 * Callers must load the tile's zone (repositories use fetch joins / entity graphs).
 */
@Component
public class TileMapper {

    public TileResponse toResponse(Tile t) {
        if (t == null) {
            return null;
        }
        Zone z = t.getZone();
        return new TileResponse(t.getId(), t.getTileCode(),
                z != null ? z.getId() : null, z != null ? z.getCode() : null,
                t.getTileType(), t.getStatus(), t.getGridRow(), t.getGridCol(), t.getPosX(), t.getPosY(),
                t.getHealthPct(), t.getStepsToday(), t.getEnergyTodayJ(), t.getLastStepAt());
    }

    public TileDetailResponse toDetail(Tile t) {
        if (t == null) {
            return null;
        }
        Zone z = t.getZone();
        return new TileDetailResponse(t.getId(), t.getTileCode(),
                z != null ? z.getId() : null, z != null ? z.getCode() : null, z != null ? z.getName() : null,
                t.getTileType(), t.getStatus(), t.getGridRow(), t.getGridCol(), t.getPosX(), t.getPosY(),
                t.getRatedJoulesPerStep(), t.getHealthPct(),
                t.getStepsToday(), t.getEnergyTodayJ(), EnergyUnits.toWh(t.getEnergyTodayJ()),
                t.getTotalSteps(), t.getTotalEnergyJ(), EnergyUnits.toWh(t.getTotalEnergyJ()),
                t.getInstalledOn(), t.getLastMaintenanceOn(), t.getLastStepAt(), t.getUpdatedAt());
    }
}
