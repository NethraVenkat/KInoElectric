package com.backend.javaleap.service;

import com.backend.javaleap.entity.Station;
import com.backend.javaleap.model.AppProperties;
import com.backend.javaleap.repository.ReadingRepository;
import com.backend.javaleap.repository.StationRepository;
import com.backend.javaleap.repository.TelemetryBatchRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.ZoneReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/** Nightly counter reset and data retention for the kinetic network. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KineticMaintenanceScheduler {

    private final TelemetryService telemetryService;
    private final TransactionTemplate transactionTemplate;
    private final StationRepository stationRepository;
    private final TileRepository tileRepository;
    private final ReadingRepository readingRepository;
    private final ZoneReadingRepository zoneReadingRepository;
    private final TelemetryBatchRepository batchRepository;
    private final AppProperties appProperties;

    /** Midnight IST: reset every tile's "today" counters (daily totals stay in energy_generation_daily). */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void resetDailyCounters() {
        int reset = telemetryService.runExclusive(() -> transactionTemplate.execute(status -> {
            int total = 0;
            for (Station s : stationRepository.findAll()) {
                total += tileRepository.resetDailyCounters(s.getId());
            }
            return total;
        }));
        log.info("Daily reset: {} tile counters cleared", reset);
    }

    /** Hourly: delete raw readings older than the retention window. */
    @Scheduled(cron = "0 7 * * * *", zone = "Asia/Kolkata")
    public void pruneRawData() {
        AppProperties.Kinetic cfg = appProperties.kinetic();
        LocalDateTime readingCutoff = LocalDateTime.now().minusHours(cfg.readingRetentionHours());
        LocalDateTime batchCutoff = LocalDateTime.now().minusDays(cfg.telemetryRetentionDays());
        int[] deleted = telemetryService.runExclusive(() -> transactionTemplate.execute(status -> new int[]{
                readingRepository.deleteOlderThan(readingCutoff),
                zoneReadingRepository.deleteOlderThan(readingCutoff),
                batchRepository.deleteOlderThan(batchCutoff)}));
        if (deleted != null && (deleted[0] + deleted[1] + deleted[2]) > 0) {
            log.info("Retention: removed {} readings, {} zone readings, {} telemetry batches",
                    deleted[0], deleted[1], deleted[2]);
        }
    }
}
