package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {

    /** Readings for the live power chart (e.g. last 60 minutes). */
    List<Reading> findByStationIdAndRecordedAtAfterOrderByRecordedAtAsc(Long stationId, LocalDateTime after);

    Optional<Reading> findTopByStationIdOrderByRecordedAtDesc(Long stationId);

    /** Retention: delete raw readings older than the cutoff. */
    @Modifying
    @Query("delete from Reading r where r.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
