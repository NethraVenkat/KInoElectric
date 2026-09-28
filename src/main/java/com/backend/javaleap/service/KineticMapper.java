package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.ApiClient;
import com.backend.javaleap.entity.EnergyGenerationDaily;
import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.TelemetryBatch;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.response.ApiKeyResponse;
import com.backend.javaleap.model.response.ConversionRateResponse;
import com.backend.javaleap.model.response.DailyGenerationResponse;
import com.backend.javaleap.model.response.SectorResponse;
import com.backend.javaleap.model.response.TelemetryBatchResponse;
import com.backend.javaleap.repository.projection.DailyGenerationView;
import com.backend.javaleap.repository.projection.ZoneStatsView;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Entity/projection -> DTO for the kinetic energy module. */
@Component
public class KineticMapper {

    public SectorResponse toResponse(Zone z, ZoneStatsView stats, AnalyticAccount account) {
        BigDecimal energyJ = stats == null || stats.getEnergyTodayJ() == null ? BigDecimal.ZERO : stats.getEnergyTodayJ();
        return new SectorResponse(z.getId(), z.getStation().getCode(), z.getCode(), z.getName(), z.getZoneType(),
                z.getTrafficWeight(), z.getMaxCapacity(), z.getPosX(), z.getPosY(), z.getWidth(), z.getHeight(),
                z.getGridRows(), z.getGridCols(),
                stats == null ? 0 : nz(stats.getTileCount()),
                stats == null ? 0 : nz(stats.getActiveTiles()),
                stats == null ? 0 : nz(stats.getFaultyTiles()),
                stats == null ? 0 : nz(stats.getStepsToday()),
                EnergyUnits.toWh(energyJ),
                EnergyUnits.round2(stats == null ? null : stats.getAvgHealthPct()),
                account == null ? null : account.getCode(),
                account == null ? null : account.getName());
    }

    public ConversionRateResponse toResponse(KineticConversionRate r, long tiles) {
        return new ConversionRateResponse(r.getTileType(), r.getJoulesPerStep(), r.getConversionEfficiency(),
                r.getEffectiveJoulesPerStep(), r.getDescription(),
                r.getUpdatedBy() == null ? null : r.getUpdatedBy().getUsername(),
                r.getUpdatedAt(), tiles);
    }

    public TelemetryBatchResponse toResponse(TelemetryBatch b) {
        return new TelemetryBatchResponse(b.getId(), b.getBatchRef(), b.getSource(), b.getRecordedAt(),
                b.getReceivedAt(), b.getIntervalSeconds(), b.getSectorCount(), b.getSteps(),
                EnergyUnits.toWh(b.getEnergyJ()), EnergyUnits.toWh(b.getStoredJ()), EnergyUnits.toWh(b.getExportedJ()),
                b.getSubmittedBy() == null ? null : b.getSubmittedBy().getUsername());
    }

    public DailyGenerationResponse toResponse(EnergyGenerationDaily g) {
        return new DailyGenerationResponse(g.getGenDate(), g.getZone().getCode(), g.getSteps(),
                kwh(g.getEnergyJ()), kwh(g.getStoredJ()), kwh(g.getExportedJ()), g.getPeakPowerW());
    }

    public DailyGenerationResponse toResponse(DailyGenerationView v) {
        return new DailyGenerationResponse(v.getGenDate(), null, nz(v.getSteps()),
                kwh(v.getEnergyJ()), kwh(v.getStoredJ()), kwh(v.getExportedJ()),
                v.getPeakPowerW() == null ? BigDecimal.ZERO : v.getPeakPowerW());
    }

    public ApiKeyResponse toResponse(ApiClient c) {
        return new ApiKeyResponse(c.getId(), c.getName(), c.getKeyPrefix() + "••••",
                c.getUser() == null ? null : c.getUser().getUsername(),
                Boolean.TRUE.equals(c.getEnabled()), c.getExpiresAt(), c.getLastUsedAt(), c.getCreatedAt());
    }

    public static BigDecimal kwh(BigDecimal joules) {
        return EnergyUnits.toKwh(joules).setScale(4, RoundingMode.HALF_UP);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
