package com.backend.javaleap.repository;

import com.backend.javaleap.entity.ZoneReading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ZoneReadingRepository extends JpaRepository<ZoneReading, Long> {

    List<ZoneReading> findByZoneIdAndRecordedAtAfterOrderByRecordedAtAsc(Long zoneId, LocalDateTime after);

    /** Latest snapshot of every zone in a station (crowd density map). */
    @Query("""
            select zr from ZoneReading zr
            join fetch zr.zone z
            where z.station.id = :stationId
              and zr.recordedAt = (select max(x.recordedAt) from ZoneReading x where x.zone.id = z.id)
            order by z.displayOrder
            """)
    List<ZoneReading> findLatestByStation(@Param("stationId") Long stationId);

    @Modifying
    @Query("delete from ZoneReading zr where zr.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
