package com.backend.javaleap.service;

import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.entity.EnergyGenerationDaily;
import com.backend.javaleap.entity.EnergySummaryHourly;
import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.Reading;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.TelemetryBatch;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.ZoneReading;
import com.backend.javaleap.entity.enums.AlertSeverity;
import com.backend.javaleap.entity.enums.AlertType;
import com.backend.javaleap.entity.enums.BatteryStatus;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.model.AppProperties;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.KineticProfiles;
import com.backend.javaleap.model.request.SectorTelemetryRequest;
import com.backend.javaleap.model.request.TelemetryRequest;
import com.backend.javaleap.model.request.TileTelemetryRequest;
import com.backend.javaleap.model.response.SectorTelemetryResult;
import com.backend.javaleap.model.response.TelemetryResultResponse;
import com.backend.javaleap.repository.BatteryRepository;
import com.backend.javaleap.repository.EnergyGenerationDailyRepository;
import com.backend.javaleap.repository.EnergySummaryHourlyRepository;
import com.backend.javaleap.repository.ReadingRepository;
import com.backend.javaleap.repository.TelemetryBatchRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.ZoneReadingRepository;
import com.backend.javaleap.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Converts one telemetry batch into energy and updates the whole kinetic model in a single transaction:
 * <ol>
 *   <li>steps per tile (explicit, or spread over the sector's working tiles)</li>
 *   <li>energy per tile = steps x rated J x conversion efficiency x health</li>
 *   <li>energy routing: batteries up to the export threshold, surplus exported to the grid</li>
 *   <li>readings, zone crowd density (Little's Law), daily generation log, hourly roll-up</li>
 *   <li>alerts: tile faults / degradation, overcrowding, low battery</li>
 * </ol>
 * Always called through {@link TelemetryService}, which serialises batches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryProcessor {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final int AVG_TILE_STEPS_PER_PASSENGER = 6;

    private final StationLookup stationLookup;
    private final ZoneRepository zoneRepository;
    private final TileRepository tileRepository;
    private final BatteryRepository batteryRepository;
    private final ReadingRepository readingRepository;
    private final ZoneReadingRepository zoneReadingRepository;
    private final EnergyGenerationDailyRepository dailyRepository;
    private final EnergySummaryHourlyRepository hourlyRepository;
    private final TelemetryBatchRepository batchRepository;
    private final UserRepository userRepository;
    private final ConversionRateService conversionRateService;
    private final AlertService alertService;
    private final AppProperties appProperties;

    /** Per-sector working data while a batch is processed. */
    private static final class SectorWork {
        final Zone zone;
        long steps;
        long lostSteps;
        BigDecimal energyJ = BigDecimal.ZERO;
        int tilesUpdated;
        int occupancy;
        BigDecimal densityPct = BigDecimal.ZERO;

        SectorWork(Zone zone) {
            this.zone = zone;
        }
    }

    @Transactional
    public TelemetryResultResponse process(TelemetryRequest req, Long submittedByUserId) {
        Station station = stationLookup.resolve(req.stationCode());

        // ---- idempotency
        String batchRef = blankToNull(req.batchRef());
        if (batchRef != null) {
            var existing = batchRepository.findByBatchRef(batchRef);
            if (existing.isPresent()) {
                return duplicate(existing.get(), station);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recordedAt = req.recordedAt() == null ? now : req.recordedAt();
        if (recordedAt.isAfter(now.plusMinutes(5))) {
            throw new BadRequestException("recordedAt cannot be in the future");
        }
        if (recordedAt.isBefore(now.minusDays(7))) {
            throw new BadRequestException("recordedAt is older than 7 days - telemetry rejected");
        }
        int interval = req.intervalSeconds() == null ? 60 : req.intervalSeconds();
        AppProperties.Kinetic cfg = appProperties.kinetic();

        // ---- load the station model once
        List<Tile> allTiles = tileRepository.findAllByStationWithZone(station.getId());
        Map<String, Zone> zonesByCode = zoneRepository.findByStationIdOrderByDisplayOrderAsc(station.getId()).stream()
                .collect(Collectors.toMap(Zone::getCode, z -> z));
        Map<String, Tile> tilesByCode = allTiles.stream().collect(Collectors.toMap(Tile::getTileCode, t -> t));
        Map<Long, List<Tile>> tilesByZone = allTiles.stream().collect(Collectors.groupingBy(t -> t.getZone().getId()));
        Map<TileType, KineticConversionRate> rates = conversionRateService.rates();

        int alerts = 0;
        Set<String> seen = new HashSet<>();
        List<SectorWork> works = new ArrayList<>();

        // ---- 1+2: steps and energy per sector / tile
        for (SectorTelemetryRequest sr : req.sectors()) {
            String code = sr.sectorCode().trim().toUpperCase(Locale.ROOT);
            if (!seen.add(code)) {
                throw new BadRequestException("Sector " + code + " appears twice in the batch");
            }
            Zone zone = zonesByCode.get(code);
            if (zone == null) {
                throw new BadRequestException("Unknown sector " + code + " for station " + station.getCode());
            }
            SectorWork work = new SectorWork(zone);
            List<Tile> zoneTiles = tilesByZone.getOrDefault(zone.getId(), List.of());
            Map<Tile, Long> stepsByTile = new LinkedHashMap<>();

            // explicit tile readings (steps, health, fault)
            if (sr.tiles() != null) {
                for (TileTelemetryRequest tr : sr.tiles()) {
                    Tile tile = tilesByCode.get(tr.tileCode().trim());
                    if (tile == null || !tile.getZone().getId().equals(zone.getId())) {
                        throw new BadRequestException("Tile " + tr.tileCode() + " does not belong to sector " + code);
                    }
                    alerts += applyDiagnostics(station, tile, tr, cfg);
                    if (tr.steps() != null && tr.steps() > 0) {
                        stepsByTile.merge(tile, tr.steps().longValue(), Long::sum);
                    }
                }
            }

            // sector-level steps spread over working tiles
            long sectorSteps = sr.steps() == null ? 0 : sr.steps();
            if (sectorSteps > 0) {
                List<Tile> working = zoneTiles.stream().filter(Tile::isOperational).toList();
                if (working.isEmpty()) {
                    work.steps += sectorSteps;
                    work.lostSteps += sectorSteps;
                } else {
                    distribute(sectorSteps, working).forEach((t, s) -> stepsByTile.merge(t, s, Long::sum));
                }
            }

            for (Map.Entry<Tile, Long> e : stepsByTile.entrySet()) {
                Tile tile = e.getKey();
                long steps = e.getValue();
                work.steps += steps;
                if (!tile.isOperational()) {
                    work.lostSteps += steps;
                    continue;
                }
                BigDecimal joules = energyFor(tile, steps, rates.get(tile.getTileType()));
                tile.registerSteps(Math.toIntExact(steps), joules, recordedAt);
                work.energyJ = work.energyJ.add(joules);
                work.tilesUpdated++;
            }

            // crowd density - Little's Law: L = arrival rate x dwell time
            double arrivals = work.steps / KineticProfiles.stepsPerPerson(zone.getZoneType());
            work.occupancy = (int) Math.round(arrivals / interval * KineticProfiles.dwellSeconds(zone.getZoneType()));
            work.densityPct = BigDecimal.valueOf(Math.min(999.99, work.occupancy * 100.0 / zone.getMaxCapacity()))
                    .setScale(2, RoundingMode.HALF_UP);
            alerts += crowdAlerts(station, zone, work.densityPct, cfg);
            works.add(work);
        }

        long totalSteps = works.stream().mapToLong(w -> w.steps).sum();
        BigDecimal totalJ = works.stream().map(w -> w.energyJ).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        // ---- 3: energy routing (batteries first, surplus exported)
        List<Battery> batteries = batteryRepository.findByStationIdOrderByCodeAsc(station.getId());
        BigDecimal[] routed = route(totalJ.divide(SECONDS_PER_HOUR, 9, RoundingMode.HALF_UP), batteries, cfg);
        BigDecimal storedJ = EnergyUnits.whToJoules(routed[0]).setScale(3, RoundingMode.HALF_UP).min(totalJ);
        BigDecimal exportedJ = totalJ.subtract(storedJ);
        BigDecimal batteryPct = averageCharge(batteries);
        BatteryStatus batteryStatus = batteries.isEmpty() ? null : batteries.get(0).getStatus();
        for (Battery b : batteries) {
            if (b.getChargePct().compareTo(BigDecimal.valueOf(20)) < 0) {
                alerts += alertService.raiseStationAlert(station, AlertType.BATTERY_LOW, AlertSeverity.WARNING,
                        "Battery " + b.getCode() + " low", "Charge at " + b.getChargePct() + "%.") ? 1 : 0;
            }
        }

        BigDecimal powerW = totalJ.divide(BigDecimal.valueOf(interval), 3, RoundingMode.HALF_UP);

        // ---- 4: persistence of readings, daily log, hourly roll-up
        for (SectorWork w : works) {
            BigDecimal share = totalJ.signum() == 0 ? BigDecimal.ZERO
                    : w.energyJ.divide(totalJ, 8, RoundingMode.HALF_UP);
            BigDecimal sectorStored = storedJ.multiply(share);
            BigDecimal sectorExported = w.energyJ.subtract(sectorStored).max(BigDecimal.ZERO);

            zoneReadingRepository.save(ZoneReading.builder()
                    .zone(w.zone).recordedAt(recordedAt)
                    .steps((int) Math.min(Integer.MAX_VALUE, w.steps))
                    .energyJ(w.energyJ.setScale(3, RoundingMode.HALF_UP))
                    .occupancy(w.occupancy).densityPct(w.densityPct)
                    .build());

            EnergyGenerationDaily daily = dailyRepository.findByZoneIdAndGenDate(w.zone.getId(), recordedAt.toLocalDate())
                    .orElseGet(() -> EnergyGenerationDaily.builder()
                            .station(station).zone(w.zone).genDate(recordedAt.toLocalDate()).build());
            daily.accumulate(w.steps - w.lostSteps, w.energyJ, sectorStored, sectorExported,
                    w.energyJ.divide(BigDecimal.valueOf(interval), 3, RoundingMode.HALF_UP));
            dailyRepository.save(daily);
        }

        readingRepository.save(Reading.builder()
                .station(station).recordedAt(recordedAt)
                .steps((int) Math.min(Integer.MAX_VALUE, totalSteps))
                .energyJ(totalJ).powerW(powerW).batteryPct(batteryPct)
                .activeTiles((int) allTiles.stream().filter(Tile::isOperational).count())
                .build());

        updateHourly(station, recordedAt, totalSteps, totalJ, powerW);

        User submitter = submittedByUserId == null ? null : userRepository.findById(submittedByUserId).orElse(null);
        TelemetryBatch batch = batchRepository.save(TelemetryBatch.builder()
                .station(station).batchRef(batchRef)
                .source(req.source() == null || req.source().isBlank() ? "API" : req.source().trim())
                .recordedAt(recordedAt).intervalSeconds(interval).sectorCount(works.size())
                .steps(totalSteps).energyJ(totalJ).storedJ(storedJ).exportedJ(exportedJ)
                .submittedBy(submitter)
                .build());

        log.debug("Telemetry {} for {}: {} steps, {} J, stored {} J, exported {} J",
                batch.getId(), station.getCode(), totalSteps, totalJ, storedJ, exportedJ);

        return new TelemetryResultResponse(batch.getId(), false, station.getCode(), recordedAt, interval,
                totalSteps, totalJ, EnergyUnits.toWh(totalJ), EnergyUnits.toKwh(totalJ), powerW,
                EnergyUnits.toWh(storedJ), EnergyUnits.toWh(exportedJ), batteryPct, batteryStatus, alerts,
                works.stream().map(w -> new SectorTelemetryResult(w.zone.getCode(), w.steps, w.lostSteps,
                        w.energyJ.setScale(3, RoundingMode.HALF_UP), EnergyUnits.toWh(w.energyJ),
                        w.occupancy, w.densityPct, w.tilesUpdated)).toList());
    }

    // ------------------------------------------------------------------ energy model

    /** Electrical energy for {@code steps} on a tile: steps x rated J x efficiency x health%. */
    static BigDecimal energyFor(Tile tile, long steps, KineticConversionRate rate) {
        return BigDecimal.valueOf(steps)
                .multiply(tile.getRatedJoulesPerStep())
                .multiply(rate.getConversionEfficiency())
                .multiply(tile.getHealthPct())
                .divide(HUNDRED, 3, RoundingMode.HALF_UP);
    }

    /** Spreads steps evenly over tiles; the remainder goes to random tiles. */
    static Map<Tile, Long> distribute(long steps, List<Tile> tiles) {
        Map<Tile, Long> result = new HashMap<>();
        long base = steps / tiles.size();
        int remainder = (int) (steps % tiles.size());
        List<Tile> shuffled = new ArrayList<>(tiles);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        for (int i = 0; i < shuffled.size(); i++) {
            long s = base + (i < remainder ? 1 : 0);
            if (s > 0) {
                result.put(shuffled.get(i), s);
            }
        }
        return result;
    }

    /**
     * Routes harvested energy: each battery is charged (with charge losses) up to the export
     * threshold; whatever is left is exported to the regional grid.
     *
     * @return [0] Wh taken by batteries (input side), [1] Wh exported
     */
    static BigDecimal[] route(BigDecimal harvestedWh, List<Battery> batteries, AppProperties.Kinetic cfg) {
        BigDecimal efficiency = BigDecimal.valueOf(cfg.batteryChargeEfficiency());
        BigDecimal threshold = BigDecimal.valueOf(cfg.exportThresholdPct()).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        BigDecimal remaining = harvestedWh;
        BigDecimal taken = BigDecimal.ZERO;
        for (Battery b : batteries) {
            BigDecimal targetWh = b.getCapacityWh().multiply(threshold);
            BigDecimal roomWh = targetWh.subtract(b.getChargeWh());
            BigDecimal applied = BigDecimal.ZERO;
            if (remaining.signum() > 0 && roomWh.signum() > 0) {
                BigDecimal input = remaining.min(roomWh.divide(efficiency, 6, RoundingMode.HALF_UP));
                applied = b.applyEnergyWh(input.multiply(efficiency));
                taken = taken.add(input);
                remaining = remaining.subtract(input);
            }
            b.setStatus(statusOf(b, applied));
        }
        return new BigDecimal[]{taken.setScale(6, RoundingMode.HALF_UP), remaining.max(BigDecimal.ZERO)};
    }

    private static BatteryStatus statusOf(Battery b, BigDecimal appliedWh) {
        BigDecimal pct = b.getChargePct();
        if (pct.compareTo(new BigDecimal("99.5")) >= 0) {
            return BatteryStatus.FULL;
        }
        if (appliedWh.signum() > 0) {
            return BatteryStatus.CHARGING;
        }
        return pct.compareTo(BigDecimal.valueOf(20)) < 0 ? BatteryStatus.LOW : BatteryStatus.IDLE;
    }

    private static BigDecimal averageCharge(List<Battery> batteries) {
        if (batteries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal cap = batteries.stream().map(Battery::getCapacityWh).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal chg = batteries.stream().map(Battery::getChargeWh).reduce(BigDecimal.ZERO, BigDecimal::add);
        return cap.signum() == 0 ? BigDecimal.ZERO : chg.multiply(HUNDRED).divide(cap, 2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ diagnostics & alerts

    /** Applies self-reported health / fault from a tile and raises alerts. */
    private int applyDiagnostics(Station station, Tile tile, TileTelemetryRequest tr, AppProperties.Kinetic cfg) {
        int raised = 0;
        if (tr.healthPct() != null) {
            tile.setHealthPct(tr.healthPct().setScale(2, RoundingMode.HALF_UP));
        }
        if (Boolean.TRUE.equals(tr.fault()) && tile.getStatus() != TileStatus.FAULTY) {
            tile.setStatus(TileStatus.FAULTY);
            raised += alertService.raiseTileAlert(station, tile, AlertType.TILE_FAULT, AlertSeverity.CRITICAL,
                    "Tile " + tile.getTileCode() + " not generating",
                    "Tile reported a transducer fault (health " + tile.getHealthPct()
                            + "%). Raise a repair PO for the tile vendor.") ? 1 : 0;
        } else if (tile.isOperational()
                && tile.getHealthPct().compareTo(BigDecimal.valueOf(cfg.degradedHealthPct())) < 0) {
            raised += alertService.raiseTileAlert(station, tile, AlertType.TILE_DEGRADED, AlertSeverity.WARNING,
                    "Tile " + tile.getTileCode() + " degrading",
                    "Tile health at " + tile.getHealthPct() + "%. Schedule a transducer overhaul.") ? 1 : 0;
        }
        return raised;
    }

    private int crowdAlerts(Station station, Zone zone, BigDecimal densityPct, AppProperties.Kinetic cfg) {
        double density = densityPct.doubleValue();
        if (density >= cfg.overcrowdingDensityPct()) {
            AlertSeverity sev = density >= 100 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
            return alertService.raiseZoneAlert(station, zone, AlertType.ZONE_OVERCROWDED, sev,
                    zone.getName() + " crowd density " + densityPct + "%",
                    "Estimated occupancy above safe level (" + zone.getMaxCapacity() + " people). Consider crowd control.")
                    ? 1 : 0;
        }
        if (density < cfg.overcrowdingDensityPct() - 15) {
            alertService.resolveZoneAlerts(zone, AlertType.ZONE_OVERCROWDED);
        }
        return 0;
    }

    // ------------------------------------------------------------------ roll-ups

    private void updateHourly(Station station, LocalDateTime recordedAt, long steps, BigDecimal energyJ,
                              BigDecimal powerW) {
        LocalDateTime hour = recordedAt.truncatedTo(ChronoUnit.HOURS);
        EnergySummaryHourly h = hourlyRepository.findByStationIdAndHourStart(station.getId(), hour)
                .orElseGet(() -> EnergySummaryHourly.builder()
                        .station(station).hourStart(hour).steps(0L).energyJ(BigDecimal.ZERO)
                        .avgPowerW(BigDecimal.ZERO).peakPowerW(BigDecimal.ZERO).footfall(0).build());
        h.setSteps(h.getSteps() + steps);
        h.setEnergyJ(h.getEnergyJ().add(energyJ).setScale(3, RoundingMode.HALF_UP));
        h.setAvgPowerW(h.getEnergyJ().divide(SECONDS_PER_HOUR, 3, RoundingMode.HALF_UP));
        h.setPeakPowerW(h.getPeakPowerW().max(powerW));
        h.setFootfall((int) Math.min(Integer.MAX_VALUE, h.getFootfall() + steps / AVG_TILE_STEPS_PER_PASSENGER));
        hourlyRepository.save(h);
    }

    private TelemetryResultResponse duplicate(TelemetryBatch b, Station station) {
        return new TelemetryResultResponse(b.getId(), true, station.getCode(), b.getRecordedAt(),
                b.getIntervalSeconds(), b.getSteps(), b.getEnergyJ(), EnergyUnits.toWh(b.getEnergyJ()),
                EnergyUnits.toKwh(b.getEnergyJ()),
                b.getEnergyJ().divide(BigDecimal.valueOf(b.getIntervalSeconds()), 3, RoundingMode.HALF_UP),
                EnergyUnits.toWh(b.getStoredJ()), EnergyUnits.toWh(b.getExportedJ()),
                averageCharge(batteryRepository.findByStationIdOrderByCodeAsc(station.getId())),
                null, 0, List.of());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
