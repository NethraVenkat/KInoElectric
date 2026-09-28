package com.backend.javaleap.service;

import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.response.SystemSummaryResponse;
import com.backend.javaleap.repository.AlertRepository;
import com.backend.javaleap.repository.BatteryRepository;
import com.backend.javaleap.repository.EnergySummaryHourlyRepository;
import com.backend.javaleap.repository.StationRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.ZoneRepository;
import com.backend.javaleap.repository.projection.StationTotalsView;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Reports what is stored in the database. Used by the startup data check and
 * GET /api/system/summary to prove the whole data layer (entities, queries, projections) works.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemService {

    private final StationRepository stationRepository;
    private final ZoneRepository zoneRepository;
    private final TileRepository tileRepository;
    private final BatteryRepository batteryRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final EnergySummaryHourlyRepository hourlyRepository;

    public SystemSummaryResponse getSummary() {
        Optional<Station> first = stationRepository.findAllByOrderByNameAsc().stream().findFirst();
        if (first.isEmpty()) {
            return new SystemSummaryResponse(null, null, 0, 0, 0, 0, 0, BigDecimal.ZERO, 0,
                    BigDecimal.ZERO, userRepository.count(), 0, 0);
        }
        Station station = first.get();
        Long id = station.getId();

        StationTotalsView totals = tileRepository.findStationTotals(id);
        BigDecimal batteryPct = batteryRepository.findFirstByStationIdOrderByIdAsc(id)
                .map(Battery::getChargePct)
                .orElse(BigDecimal.ZERO);

        return new SystemSummaryResponse(
                station.getCode(),
                station.getName(),
                zoneRepository.countByStationId(id),
                nz(totals.getTotalTiles()),
                nz(totals.getActiveTiles()),
                nz(totals.getFaultyTiles()),
                nz(totals.getMaintenanceTiles()),
                EnergyUnits.round2(totals.getAvgHealthPct()),
                batteryRepository.findByStationIdOrderByCodeAsc(id).size(),
                batteryPct,
                userRepository.count(),
                alertRepository.countByStationIdAndStatusIn(id,
                        EnumSet.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED)),
                hourlyRepository.countByStationId(id));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
