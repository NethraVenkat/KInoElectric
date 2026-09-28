package com.backend.javaleap.service;

import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.enums.AlertSeverity;
import com.backend.javaleap.entity.enums.AlertType;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.model.TilePattern;
import com.backend.javaleap.model.request.ConversionRateUpdateRequest;
import com.backend.javaleap.model.request.SectorCreateRequest;
import com.backend.javaleap.model.request.SectorTelemetryRequest;
import com.backend.javaleap.model.request.TelemetryRequest;
import com.backend.javaleap.model.request.TileStatusUpdateRequest;
import com.backend.javaleap.model.request.TileTelemetryRequest;
import com.backend.javaleap.model.response.SectorDetailResponse;
import com.backend.javaleap.model.response.TelemetryResultResponse;
import com.backend.javaleap.repository.AlertRepository;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.BatteryRepository;
import com.backend.javaleap.repository.EnergyGenerationDailyRepository;
import com.backend.javaleap.repository.ReadingRepository;
import com.backend.javaleap.repository.TileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** End-to-end tests of sectors, conversion rates, telemetry and the simulator on H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KineticIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired TelemetryService telemetryService;
    @Autowired SectorService sectorService;
    @Autowired ConversionRateService conversionRateService;
    @Autowired SimulationService simulationService;
    @Autowired TileRepository tileRepository;
    @Autowired BatteryRepository batteryRepository;
    @Autowired EnergyGenerationDailyRepository dailyRepository;
    @Autowired ReadingRepository readingRepository;
    @Autowired AlertRepository alertRepository;
    @Autowired AnalyticAccountRepository analyticAccountRepository;

    KineticTestData data;

    @BeforeEach
    void setUp() {
        data = new KineticTestData(ctx);
    }

    private TelemetryResultResponse send(String batchRef, SectorTelemetryRequest... sectors) {
        return telemetryService.ingest(new TelemetryRequest("TST", batchRef, "TEST", null, 60, List.of(sectors)),
                data.system.getId());
    }

    private long stepsToday(Long zoneId) {
        return tileRepository.findByZoneIdOrderByGridRowAscGridColAsc(zoneId).stream()
                .mapToLong(Tile::getStepsToday).sum();
    }

    @Test
    void sectorStepsBecomeEnergyStoredInBattery() {
        TelemetryResultResponse r = send(null, new SectorTelemetryRequest("ENT", 100L, null));

        // 100 steps x 2.5 J x 0.9 efficiency x 100% health = 225 J
        assertThat(r.totalSteps()).isEqualTo(100);
        assertThat(r.energyJ()).isEqualByComparingTo("225.000");
        assertThat(r.averagePowerW()).isEqualByComparingTo("3.750");
        assertThat(r.exportedWh()).isEqualByComparingTo("0");
        assertThat(stepsToday(data.entry.getId())).isEqualTo(100);

        // battery gained 225 J = 0.0625 Wh x 95% charge efficiency
        assertThat(batteryRepository.findById(data.battery.getId()).orElseThrow().getChargeWh())
                .isEqualByComparingTo("1000.059");

        var daily = dailyRepository.findByZoneIdAndGenDate(data.entry.getId(), LocalDate.now()).orElseThrow();
        assertThat(daily.getSteps()).isEqualTo(100);
        assertThat(daily.getEnergyJ()).isEqualByComparingTo("225.000");
        assertThat(readingRepository.findTopByStationIdOrderByRecordedAtDesc(data.station.getId())).isPresent();

        // Little's Law: 100/3 people per 60 s x 20 s dwell = 11 people -> 11% of capacity 100
        assertThat(r.sectors().get(0).occupancy()).isEqualTo(11);
    }

    @Test
    void surplusIsExportedWhenBatteryAtThreshold() {
        var b = batteryRepository.findById(data.battery.getId()).orElseThrow();
        b.setChargeWh(new BigDecimal("1600.000"));   // 80% = export threshold

        TelemetryResultResponse r = send(null, new SectorTelemetryRequest("ENT", 400L, null));
        assertThat(r.storedWh()).isEqualByComparingTo("0");
        assertThat(r.exportedWh()).isEqualByComparingTo(r.energyWh());
        assertThat(dailyRepository.findByZoneIdAndGenDate(data.entry.getId(), LocalDate.now()).orElseThrow()
                .getExportedJ()).isEqualByComparingTo("900.000");
    }

    @Test
    void duplicateBatchRefIsProcessedOnlyOnce() {
        send("GW-1-0001", new SectorTelemetryRequest("ENT", 100L, null));
        TelemetryResultResponse again = send("GW-1-0001", new SectorTelemetryRequest("ENT", 100L, null));
        assertThat(again.duplicate()).isTrue();
        assertThat(stepsToday(data.entry.getId())).isEqualTo(100);
    }

    @Test
    void faultyTileRaisesAlertAndLosesSteps() {
        TelemetryResultResponse r = send(null, new SectorTelemetryRequest("ENT", null,
                List.of(new TileTelemetryRequest("TST-ENT-R1C01", 10, new BigDecimal("12"), true))));

        Tile tile = tileRepository.findByTileCode("TST-ENT-R1C01").orElseThrow();
        assertThat(tile.getStatus()).isEqualTo(TileStatus.FAULTY);
        assertThat(r.sectors().get(0).lostSteps()).isEqualTo(10);
        assertThat(r.energyJ()).isEqualByComparingTo("0");
        assertThat(alertRepository.existsByTileIdAndAlertTypeAndStatusIn(tile.getId(), AlertType.TILE_FAULT,
                AlertService.UNRESOLVED)).isTrue();

        // repair -> ACTIVE restores health and resolves the alert
        sectorService.updateTileStatus(tile.getId(), new TileStatusUpdateRequest(TileStatus.ACTIVE, "replaced"));
        assertThat(tile.getHealthPct()).isEqualByComparingTo("100");
        assertThat(alertRepository.existsByTileIdAndAlertTypeAndStatusIn(tile.getId(), AlertType.TILE_FAULT,
                AlertService.UNRESOLVED)).isFalse();
    }

    @Test
    void overcrowdedPlatformRaisesCriticalAlert() {
        // PF1: 100 steps / 2 per person / 60 s x 240 s dwell = 200 people = 100% of capacity 200
        TelemetryResultResponse r = send(null, new SectorTelemetryRequest("PF1", 100L, null));
        assertThat(r.sectors().get(0).densityPct()).isEqualByComparingTo("100.00");
        assertThat(alertRepository.findByZoneIdAndAlertTypeAndStatusIn(data.platform.getId(),
                AlertType.ZONE_OVERCROWDED, AlertService.UNRESOLVED))
                .singleElement().extracting("severity").isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void invalidTelemetryIsRejected() {
        assertThatThrownBy(() -> send(null, new SectorTelemetryRequest("XXX", 5L, null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> send(null, new SectorTelemetryRequest("ENT", 5L, null),
                new SectorTelemetryRequest("ENT", 5L, null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> send(null, new SectorTelemetryRequest("ENT", null,
                List.of(new TileTelemetryRequest("TST-PF1-R1C01", 1, null, null)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createSectorBuildsTileGridAndAnalyticAccount() {
        SectorDetailResponse s = sectorService.create(new SectorCreateRequest("TST", "CON", "Central Concourse",
                ZoneType.CONCOURSE, null, 300, 2, 3, TilePattern.MIXED, null, null, null, null));

        assertThat(s.tiles()).hasSize(6);
        assertThat(s.tiles().stream().filter(t -> t.tileType() == TileType.ELECTROMAGNETIC)).hasSize(2);
        assertThat(s.sector().analyticAccountCode()).isEqualTo("AA-TST-CON");
        assertThat(analyticAccountRepository.findByCode("AA-TST-CON")).isPresent();
        assertThat(s.sector().posY()).isGreaterThan(data.platform.getPosY());

        assertThatThrownBy(() -> sectorService.create(new SectorCreateRequest("TST", "CON", "Again",
                ZoneType.CONCOURSE, null, 300, 1, 1, null, null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void conversionRateCanReRateExistingTiles() {
        conversionRateService.update(TileType.PIEZO,
                new ConversionRateUpdateRequest(new BigDecimal("3.00"), new BigDecimal("0.800"), null, true));
        assertThat(tileRepository.findByTileCode("TST-ENT-R1C01").orElseThrow().getRatedJoulesPerStep())
                .isEqualByComparingTo("3.00");

        // 10 steps x 3.0 J x 0.8 = 24 J
        TelemetryResultResponse r = send(null, new SectorTelemetryRequest("ENT", 10L, null));
        assertThat(r.energyJ()).isEqualByComparingTo("24.000");
    }

    @Test
    void simulatorTickFeedsTelemetryPipeline() {
        simulationService.setSpeed(60);
        simulationService.rushHour(1);
        simulationService.tick();
        var status = simulationService.status();
        simulationService.setSpeed(1);

        assertThat(status.lastError()).isNull();
        assertThat(status.ticks()).isGreaterThanOrEqualTo(1);
        assertThat(status.rushHourActive()).isTrue();
        assertThat(readingRepository.findTopByStationIdOrderByRecordedAtDesc(data.station.getId())).isPresent();
    }
}
