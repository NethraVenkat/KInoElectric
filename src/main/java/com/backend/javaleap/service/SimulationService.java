package com.backend.javaleap.service;

import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.entity.enums.StationStatus;
import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.model.AppProperties;
import com.backend.javaleap.model.KineticProfiles;
import com.backend.javaleap.model.request.SectorTelemetryRequest;
import com.backend.javaleap.model.request.TelemetryRequest;
import com.backend.javaleap.model.request.TileTelemetryRequest;
import com.backend.javaleap.model.response.SimulationStatusResponse;
import com.backend.javaleap.model.response.TelemetryResultResponse;
import com.backend.javaleap.repository.StationRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.ZoneRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo "System" actor: generates realistic footfall and feeds it through the SAME telemetry
 * pipeline a real gateway uses ({@link TelemetryService#ingest}).
 * <p>
 * Model: passengers/hour = daily footfall x metro day curve x weekend factor x rush boost.
 * Each passenger steps on a sector's tiles {@link KineticProfiles#stepsPerPerson} times.
 * Tiles slowly wear out, and occasionally fail, so maintenance alerts appear naturally.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final double RUSH_BOOST = 3.0;
    private static final double WEEKEND_FACTOR = 0.7;

    private final AppProperties appProperties;
    private final StationRepository stationRepository;
    private final ZoneRepository zoneRepository;
    private final TileRepository tileRepository;
    private final UserRepository userRepository;
    private final TelemetryService telemetryService;

    private volatile boolean running;
    private volatile int speed = 1;
    private volatile LocalDateTime rushUntil;
    private volatile LocalDateTime lastTickAt;
    private volatile String lastError;
    private volatile int currentPassengersPerHour;
    private long ticks;
    private long stepsSimulated;
    private BigDecimal energySimulatedWh = BigDecimal.ZERO;
    private int faultsInjected;
    private Long systemUserId;

    @PostConstruct
    void init() {
        running = appProperties.simulation().enabled();
        if (running) {
            log.info("Footfall simulator enabled (tick {} ms)", appProperties.simulation().tickMs());
        }
    }

    @Scheduled(fixedDelayString = "${app.simulation.tick-ms:1500}", initialDelay = 5000)
    public void scheduledTick() {
        if (running) {
            tick();
        }
    }

    // ------------------------------------------------------------------ controls

    public synchronized SimulationStatusResponse start() {
        running = true;
        lastError = null;
        log.info("Simulator started at {}x", speed);
        return status();
    }

    public synchronized SimulationStatusResponse stop() {
        running = false;
        log.info("Simulator stopped after {} ticks", ticks);
        return status();
    }

    public synchronized SimulationStatusResponse setSpeed(int newSpeed) {
        if (newSpeed < 1 || newSpeed > 120) {
            throw new BadRequestException("speed must be between 1 and 120");
        }
        speed = newSpeed;
        return status();
    }

    public synchronized SimulationStatusResponse rushHour(int minutes) {
        int m = Math.max(1, Math.min(minutes, 30));
        rushUntil = LocalDateTime.now().plusMinutes(m);
        log.info("Rush hour simulated for {} minute(s)", m);
        return status();
    }

    /** Breaks one random working tile immediately (demo of fault alerts). */
    public synchronized SimulationStatusResponse injectFault() {
        Station station = onlineStations().stream().findFirst()
                .orElseThrow(() -> new BadRequestException("No online station"));
        List<Tile> working = tileRepository.findAllByStationWithZone(station.getId()).stream()
                .filter(Tile::isOperational).toList();
        if (working.isEmpty()) {
            throw new BadRequestException("No working tiles left to break");
        }
        Tile victim = working.get(ThreadLocalRandom.current().nextInt(working.size()));
        TileTelemetryRequest fault = new TileTelemetryRequest(victim.getTileCode(), 0,
                BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(8, 25)).setScale(2, RoundingMode.HALF_UP),
                true);
        telemetryService.ingest(new TelemetryRequest(station.getCode(), null, "SIMULATOR-FAULT", LocalDateTime.now(),
                1, List.of(new SectorTelemetryRequest(victim.getZone().getCode(), 0L, List.of(fault)))),
                systemUserId());
        faultsInjected++;
        log.info("Injected fault on tile {}", victim.getTileCode());
        return status();
    }

    public synchronized SimulationStatusResponse status() {
        boolean rush = rushUntil != null && rushUntil.isAfter(LocalDateTime.now());
        return new SimulationStatusResponse(running, speed, rush, rush ? rushUntil : null,
                appProperties.simulation().tickMs(), ticks, lastTickAt, stepsSimulated,
                energySimulatedWh.setScale(3, RoundingMode.HALF_UP), faultsInjected, currentPassengersPerHour,
                lastError);
    }

    // ------------------------------------------------------------------ one tick

    /** Simulates one tick for every online station. */
    public synchronized void tick() {
        try {
            for (Station station : onlineStations()) {
                TelemetryResultResponse result = telemetryService.ingest(buildBatch(station), systemUserId());
                stepsSimulated += result.totalSteps();
                energySimulatedWh = energySimulatedWh.add(result.energyWh());
            }
            ticks++;
            lastTickAt = LocalDateTime.now();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            log.warn("Simulator tick failed: {}", lastError);
        }
    }

    TelemetryRequest buildBatch(Station station) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();
        double virtualSeconds = appProperties.simulation().tickMs() / 1000.0 * speed;

        double perHour = station.getDailyFootfallEstimate() * KineticProfiles.hourShare(now.getHour());
        if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            perHour *= WEEKEND_FACTOR;
        }
        if (rushUntil != null && rushUntil.isAfter(now)) {
            perHour *= RUSH_BOOST;
        }
        currentPassengersPerHour = (int) Math.round(perHour);
        double passengers = perHour * virtualSeconds / 3600.0;

        List<Zone> zones = zoneRepository.findByStationIdOrderByDisplayOrderAsc(station.getId());
        double platformWeight = zones.stream().filter(z -> z.getZoneType() == ZoneType.PLATFORM)
                .mapToDouble(z -> z.getTrafficWeight().doubleValue()).sum();

        Map<String, List<TileTelemetryRequest>> tileEvents = tileWearEvents(station, rnd);

        List<SectorTelemetryRequest> sectors = new ArrayList<>();
        for (Zone z : zones) {
            double weight = z.getTrafficWeight().doubleValue();
            double share = z.getZoneType() == ZoneType.PLATFORM && platformWeight > 0
                    ? weight / platformWeight
                    : KineticProfiles.passengerShare(z.getZoneType()) * Math.min(1.0, weight);
            double expected = passengers * share * KineticProfiles.stepsPerPerson(z.getZoneType());
            long steps = Math.max(0, Math.round(expected + rnd.nextGaussian() * Math.sqrt(Math.max(expected, 0))));
            sectors.add(new SectorTelemetryRequest(z.getCode(), steps, tileEvents.get(z.getCode())));
        }
        return new TelemetryRequest(station.getCode(), null, "SIMULATOR", now,
                (int) Math.max(1, Math.round(virtualSeconds)), sectors);
    }

    /** Random wear (health -0.5..2%) and rare faults, scaled by simulation speed. */
    private Map<String, List<TileTelemetryRequest>> tileWearEvents(Station station, ThreadLocalRandom rnd) {
        Map<String, List<TileTelemetryRequest>> events = new HashMap<>();
        double wearChance = Math.min(0.5, 0.01 * speed);
        double faultChance = Math.min(0.05, 0.0005 * speed);
        boolean wear = rnd.nextDouble() < wearChance;
        boolean fault = rnd.nextDouble() < faultChance;
        if (!wear && !fault) {
            return events;
        }
        List<Tile> working = tileRepository.findAllByStationWithZone(station.getId()).stream()
                .filter(Tile::isOperational).toList();
        if (working.isEmpty()) {
            return events;
        }
        if (wear) {
            Tile t = working.get(rnd.nextInt(working.size()));
            BigDecimal health = t.getHealthPct().subtract(BigDecimal.valueOf(rnd.nextDouble(0.5, 2.0)))
                    .max(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
            events.computeIfAbsent(t.getZone().getCode(), k -> new ArrayList<>())
                    .add(new TileTelemetryRequest(t.getTileCode(), 0, health, false));
        }
        if (fault) {
            Tile t = working.get(rnd.nextInt(working.size()));
            events.computeIfAbsent(t.getZone().getCode(), k -> new ArrayList<>())
                    .add(new TileTelemetryRequest(t.getTileCode(), 0,
                            BigDecimal.valueOf(rnd.nextDouble(8, 25)).setScale(2, RoundingMode.HALF_UP), true));
            faultsInjected++;
        }
        return events;
    }

    private List<Station> onlineStations() {
        return stationRepository.findAllByOrderByNameAsc().stream()
                .filter(s -> s.getStatus() == StationStatus.ONLINE).toList();
    }

    private Long systemUserId() {
        if (systemUserId == null) {
            systemUserId = userRepository.findByRoleOrderByUsernameAsc(Role.SYSTEM).stream()
                    .findFirst().map(User::getId).orElse(null);
        }
        return systemUserId;
    }
}
