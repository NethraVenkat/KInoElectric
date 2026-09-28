package com.backend.javaleap.service;

import com.backend.javaleap.model.response.SystemSummaryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Prints a data summary on startup so you can see at a glance that
 * Flyway, entities and repositories all work together.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataIntegrityReporter implements ApplicationRunner {

    private final SystemService systemService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            SystemSummaryResponse s = systemService.getSummary();
            log.info("""

                    ================ PadaShakti data check ================
                     Station      : {} ({})
                     Zones        : {}
                     Tiles        : {} total | {} active | {} faulty | {} maintenance
                     Avg health   : {} %
                     Batteries    : {} (charge {} %)
                     Users        : {}
                     Open alerts  : {}
                     History rows : {} hourly summaries
                    =======================================================""",
                    s.stationName(), s.stationCode(), s.zones(),
                    s.tiles(), s.activeTiles(), s.faultyTiles(), s.maintenanceTiles(),
                    s.avgTileHealthPct(), s.batteries(), s.batteryChargePct(),
                    s.users(), s.openAlerts(), s.hourlyHistoryRows());
            if (s.tiles() == 0) {
                log.warn("No tiles found - check that Flyway migrations V1..V4 ran.");
            }
        } catch (Exception ex) {
            log.error("Data check failed - the data layer is not healthy", ex);
        }
    }
}
