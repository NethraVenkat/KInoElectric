package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Alert;
import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.AlertSeverity;
import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.entity.enums.AlertType;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.model.response.SystemSummaryResponse;
import com.backend.javaleap.repository.projection.AlertCountView;
import com.backend.javaleap.repository.projection.StationTotalsView;
import com.backend.javaleap.repository.projection.ZoneStatsView;
import com.backend.javaleap.service.SystemService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test of the full data layer on an in-memory H2 database:
 * entity mappings, relationships, custom JPQL queries and projections.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTest {

    @Autowired StationRepository stationRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired TileRepository tileRepository;
    @Autowired BatteryRepository batteryRepository;
    @Autowired AlertRepository alertRepository;
    @Autowired UserRepository userRepository;
    @Autowired SystemService systemService;

    Station station;
    Zone entry;
    Zone platform;

    @BeforeEach
    void seed() {
        station = stationRepository.save(Station.builder()
                .code("TST").name("Test Station").city("Chennai").build());

        entry = zoneRepository.save(zone("ENT", ZoneType.ENTRY, 1));
        platform = zoneRepository.save(zone("PF1", ZoneType.PLATFORM, 2));

        // 2 active + 1 faulty tile in entry, 1 active tile on platform
        Tile t1 = tile(entry, "TST-ENT-1", 1, TileStatus.ACTIVE);
        t1.registerSteps(10, new BigDecimal("30"), LocalDateTime.now());
        Tile t2 = tile(entry, "TST-ENT-2", 2, TileStatus.ACTIVE);
        t2.registerSteps(5, new BigDecimal("15"), LocalDateTime.now());
        Tile t3 = tile(entry, "TST-ENT-3", 3, TileStatus.FAULTY);
        t3.setHealthPct(new BigDecimal("20"));
        Tile t4 = tile(platform, "TST-PF1-1", 1, TileStatus.ACTIVE);
        tileRepository.saveAll(List.of(t1, t2, t3, t4));

        batteryRepository.save(Battery.builder().station(station).code("TST-BAT").name("Bank")
                .capacityWh(new BigDecimal("2000")).chargeWh(new BigDecimal("500"))
                .maxChargeRateW(new BigDecimal("500")).build());

        userRepository.save(User.builder().username("tester").email("t@x.in")
                .passwordHash("hash").fullName("Tester").role(Role.ACCOUNTANT).build());

        alertRepository.save(Alert.builder().station(station).zone(entry).tile(t3)
                .alertType(AlertType.TILE_FAULT).severity(AlertSeverity.CRITICAL)
                .title("Tile fault").message("t3 dead").build());
    }

    @Test
    void loadsTilesWithZoneInLayoutOrder() {
        List<Tile> tiles = tileRepository.findAllByStationWithZone(station.getId());
        assertThat(tiles).hasSize(4);
        assertThat(tiles.get(0).getZone().getCode()).isEqualTo("ENT");
        assertThat(tiles.get(3).getZone().getCode()).isEqualTo("PF1");
        assertThat(tileRepository.findByTileCode("TST-ENT-2")).isPresent();
    }

    @Test
    void zoneStatsAggregatesPerZone() {
        List<ZoneStatsView> stats = zoneRepository.findZoneStats(station.getId());
        assertThat(stats).hasSize(2);

        ZoneStatsView ent = stats.get(0);
        assertThat(ent.getCode()).isEqualTo("ENT");
        assertThat(ent.getTileCount()).isEqualTo(3L);
        assertThat(ent.getActiveTiles()).isEqualTo(2L);
        assertThat(ent.getFaultyTiles()).isEqualTo(1L);
        assertThat(ent.getStepsToday()).isEqualTo(15L);
        assertThat(ent.getEnergyTodayJ()).isEqualByComparingTo("45");
    }

    @Test
    void stationTotalsInOneQuery() {
        StationTotalsView totals = tileRepository.findStationTotals(station.getId());
        assertThat(totals.getTotalTiles()).isEqualTo(4L);
        assertThat(totals.getActiveTiles()).isEqualTo(3L);
        assertThat(totals.getFaultyTiles()).isEqualTo(1L);
        assertThat(totals.getStepsToday()).isEqualTo(15L);
        assertThat(totals.getEnergyTodayJ()).isEqualByComparingTo("45");
    }

    @Test
    void searchFiltersByStatusAndText() {
        var faulty = tileRepository.search(station.getId(), null, TileStatus.FAULTY, null, PageRequest.of(0, 10));
        assertThat(faulty.getTotalElements()).isEqualTo(1);

        var byText = tileRepository.search(station.getId(), entry.getId(), null, "ent-2", PageRequest.of(0, 10));
        assertThat(byText.getContent()).extracting(Tile::getTileCode).containsExactly("TST-ENT-2");
    }

    @Test
    void resetDailyCountersClearsTodayOnly() {
        int updated = tileRepository.resetDailyCounters(station.getId());
        assertThat(updated).isEqualTo(4);
        StationTotalsView totals = tileRepository.findStationTotals(station.getId());
        assertThat(totals.getStepsToday()).isZero();
        assertThat(totals.getTotalSteps()).isEqualTo(15L);
    }

    @Test
    void alertQueriesAndDeduplication() {
        List<AlertCountView> counts = alertRepository.countOpenBySeverity(station.getId());
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);

        Tile faulty = tileRepository.findByTileCode("TST-ENT-3").orElseThrow();
        assertThat(alertRepository.existsByTileIdAndAlertTypeAndStatusIn(faulty.getId(), AlertType.TILE_FAULT,
                EnumSet.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED))).isTrue();

        var page = alertRepository.findByStationIdOrderByCreatedAtDesc(station.getId(), PageRequest.of(0, 5));
        assertThat(page.getContent().get(0).getTile().getTileCode()).isEqualTo("TST-ENT-3");
    }

    @Test
    void userLookup() {
        assertThat(userRepository.findByUsername("tester")).isPresent();
        assertThat(userRepository.existsByEmail("t@x.in")).isTrue();
    }

    @Test
    void systemSummaryReflectsDatabase() {
        SystemSummaryResponse s = systemService.getSummary();
        assertThat(s.stationCode()).isEqualTo("TST");
        assertThat(s.zones()).isEqualTo(2);
        assertThat(s.tiles()).isEqualTo(4);
        assertThat(s.faultyTiles()).isEqualTo(1);
        assertThat(s.batteryChargePct()).isEqualByComparingTo("25.00");
        assertThat(s.openAlerts()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private Zone zone(String code, ZoneType type, int order) {
        return Zone.builder().station(station).code(code).name(code).zoneType(type)
                .maxCapacity(100).posX(0).posY(0).width(100).height(100)
                .gridRows(1).gridCols(5).displayOrder(order).build();
    }

    private Tile tile(Zone zone, String code, int col, TileStatus status) {
        return Tile.builder().zone(zone).tileCode(code).tileType(TileType.PIEZO).status(status)
                .gridRow(1).gridCol(col).posX(BigDecimal.ONE).posY(BigDecimal.ONE)
                .ratedJoulesPerStep(new BigDecimal("2.5")).installedOn(LocalDate.now()).build();
    }
}
