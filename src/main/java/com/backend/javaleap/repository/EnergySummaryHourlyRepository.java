package com.backend.javaleap.repository;

import com.backend.javaleap.entity.EnergySummaryHourly;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnergySummaryHourlyRepository extends JpaRepository<EnergySummaryHourly, Long> {

    List<EnergySummaryHourly> findByStationIdAndHourStartBetweenOrderByHourStartAsc(
            Long stationId, LocalDateTime from, LocalDateTime to);

    /** Used to upsert the current hour's roll-up. */
    Optional<EnergySummaryHourly> findByStationIdAndHourStart(Long stationId, LocalDateTime hourStart);

    long countByStationId(Long stationId);
}
