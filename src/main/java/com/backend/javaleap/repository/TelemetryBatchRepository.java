package com.backend.javaleap.repository;

import com.backend.javaleap.entity.TelemetryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryBatchRepository extends JpaRepository<TelemetryBatch, Long> {

    Optional<TelemetryBatch> findByBatchRef(String batchRef);

    List<TelemetryBatch> findTop50ByStationIdOrderByRecordedAtDesc(Long stationId);

    long countByStationId(Long stationId);

    @Modifying
    @Query("delete from TelemetryBatch b where b.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
