package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.repository.projection.ZoneStatsView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByStationIdOrderByDisplayOrderAsc(Long stationId);

    Optional<Zone> findByStationIdAndCode(Long stationId, String code);

    long countByStationId(Long stationId);

    /** Live per-zone aggregates used by the dashboard "energy by zone" chart. */
    @Query("""
            select z.id as zoneId,
                   z.code as code,
                   z.name as name,
                   z.zoneType as zoneType,
                   count(t.id) as tileCount,
                   coalesce(sum(case when t.status = com.backend.javaleap.entity.enums.TileStatus.ACTIVE then 1L else 0L end), 0L) as activeTiles,
                   coalesce(sum(case when t.status = com.backend.javaleap.entity.enums.TileStatus.FAULTY then 1L else 0L end), 0L) as faultyTiles,
                   coalesce(sum(t.stepsToday), 0L) as stepsToday,
                   coalesce(sum(t.energyTodayJ), 0) as energyTodayJ,
                   avg(t.healthPct) as avgHealthPct
            from Zone z
            left join z.tiles t
            where z.station.id = :stationId
            group by z.id, z.code, z.name, z.zoneType, z.displayOrder
            order by z.displayOrder
            """)
    List<ZoneStatsView> findZoneStats(@Param("stationId") Long stationId);
}
