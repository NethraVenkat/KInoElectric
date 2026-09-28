package com.backend.javaleap.service;

import com.backend.javaleap.entity.Station;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.model.EnergyUnits;
import com.backend.javaleap.model.request.TelemetryRequest;
import com.backend.javaleap.model.response.DailyGenerationResponse;
import com.backend.javaleap.model.response.GenerationSummaryResponse;
import com.backend.javaleap.model.response.TelemetryBatchResponse;
import com.backend.javaleap.model.response.TelemetryResultResponse;
import com.backend.javaleap.repository.EnergyGenerationDailyRepository;
import com.backend.javaleap.repository.TelemetryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Entry point for telemetry. Batches (API, gateways, simulator) and nightly maintenance are
 * serialised through one lock so tile counters and batteries are never updated concurrently.
 */
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final ReentrantLock lock = new ReentrantLock(true);

    private final TelemetryProcessor processor;
    private final StationLookup stationLookup;
    private final TelemetryBatchRepository batchRepository;
    private final EnergyGenerationDailyRepository dailyRepository;
    private final KineticMapper mapper;

    /** Processes a telemetry batch (thread-safe). */
    public TelemetryResultResponse ingest(TelemetryRequest request, Long submittedByUserId) {
        return runExclusive(() -> processor.process(request, submittedByUserId));
    }

    /** Runs work while holding the telemetry lock (used by the nightly reset). */
    public <T> T runExclusive(Supplier<T> work) {
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public List<TelemetryBatchResponse> recentBatches(String stationCode) {
        Station station = stationLookup.resolve(stationCode);
        return batchRepository.findTop50ByStationIdOrderByRecordedAtDesc(station.getId()).stream()
                .map(mapper::toResponse).toList();
    }

    /** Daily generation report - station totals, or per sector when {@code bySector} is true. */
    @Transactional(readOnly = true)
    public GenerationSummaryResponse generation(String stationCode, LocalDate from, LocalDate to, boolean bySector) {
        Station station = stationLookup.resolve(stationCode);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be on or before to");
        }
        if (ChronoUnit.DAYS.between(start, end) > 366) {
            throw new BadRequestException("Maximum range is 366 days");
        }

        List<DailyGenerationResponse> days = bySector
                ? dailyRepository.findSectorRows(station.getId(), start, end).stream().map(mapper::toResponse).toList()
                : dailyRepository.findStationDaily(station.getId(), start, end).stream().map(mapper::toResponse).toList();

        var stationRows = dailyRepository.findStationDaily(station.getId(), start, end);
        long steps = stationRows.stream().mapToLong(r -> r.getSteps() == null ? 0 : r.getSteps()).sum();
        BigDecimal energy = sum(stationRows.stream().map(r -> r.getEnergyJ()).toList());
        BigDecimal stored = sum(stationRows.stream().map(r -> r.getStoredJ()).toList());
        BigDecimal exported = sum(stationRows.stream().map(r -> r.getExportedJ()).toList());

        return new GenerationSummaryResponse(station.getCode(), start, end, steps,
                KineticMapper.kwh(energy), KineticMapper.kwh(stored), KineticMapper.kwh(exported), days);
    }

    /** kWh exported to the grid in a period - used by monthly energy sales orders (Phase D). */
    @Transactional(readOnly = true)
    public BigDecimal exportedKwh(Long stationId, LocalDate from, LocalDate to) {
        return EnergyUnits.toKwh(dailyRepository.sumExportedJ(stationId, from, to));
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
