package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.repository.projection.StationTotalsView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TileRepository extends JpaRepository<Tile, Long>, JpaSpecificationExecutor<Tile> {

    /** All tiles of a station with their zone loaded (floor map, simulator). */
    @Query("""
            select t from Tile t
            join fetch t.zone z
            where z.station.id = :stationId
            order by z.displayOrder, t.gridRow, t.gridCol
            """)
    List<Tile> findAllByStationWithZone(@Param("stationId") Long stationId);

    @EntityGraph(attributePaths = "zone")
    Optional<Tile> findWithZoneById(Long id);

    @EntityGraph(attributePaths = "zone")
    Optional<Tile> findByTileCode(String tileCode);

    @EntityGraph(attributePaths = "zone")
    List<Tile> findByZoneIdOrderByGridRowAscGridColAsc(Long zoneId);

    /** Paged + filterable tile list for the Tile Network Manager table. */
    @EntityGraph(attributePaths = "zone")
    @Query("""
            select t from Tile t
            where t.zone.station.id = :stationId
              and (:zoneId is null or t.zone.id = :zoneId)
              and (:status is null or t.status = :status)
              and (:search is null or lower(t.tileCode) like lower(concat('%', :search, '%')))
            """)
    Page<Tile> search(@Param("stationId") Long stationId,
                      @Param("zoneId") Long zoneId,
                      @Param("status") TileStatus status,
                      @Param("search") String search,
                      Pageable pageable);

    @EntityGraph(attributePaths = "zone")
    List<Tile> findByZoneStationIdAndStatusIn(Long stationId, Collection<TileStatus> statuses);

    /** Tiles whose health dropped below a threshold (predictive maintenance). */
    @EntityGraph(attributePaths = "zone")
    List<Tile> findByZoneStationIdAndHealthPctLessThanOrderByHealthPctAsc(Long stationId, BigDecimal threshold);

    long countByZoneStationId(Long stationId);

    long countByZoneStationIdAndStatus(Long stationId, TileStatus status);

    /** Station-wide totals in a single query (dashboard KPIs). */
    @Query("""
            select count(t.id) as totalTiles,
                   coalesce(sum(case when t.status = com.backend.javaleap.entity.enums.TileStatus.ACTIVE then 1L else 0L end), 0L) as activeTiles,
                   coalesce(sum(case when t.status = com.backend.javaleap.entity.enums.TileStatus.FAULTY then 1L else 0L end), 0L) as faultyTiles,
                   coalesce(sum(case when t.status = com.backend.javaleap.entity.enums.TileStatus.MAINTENANCE then 1L else 0L end), 0L) as maintenanceTiles,
                   coalesce(sum(t.stepsToday), 0L) as stepsToday,
                   coalesce(sum(t.energyTodayJ), 0) as energyTodayJ,
                   coalesce(sum(t.totalSteps), 0L) as totalSteps,
                   coalesce(sum(t.totalEnergyJ), 0) as totalEnergyJ,
                   avg(t.healthPct) as avgHealthPct
            from Tile t
            where t.zone.station.id = :stationId
            """)
    StationTotalsView findStationTotals(@Param("stationId") Long stationId);

    /** Midnight reset of daily counters for every tile of a station. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Tile t set t.stepsToday = 0, t.energyTodayJ = 0
            where t.zone.id in (select z.id from Zone z where z.station.id = :stationId)
            """)
    int resetDailyCounters(@Param("stationId") Long stationId);

    long countByTileType(TileType tileType);

    /** Re-rate every tile of a technology after a conversion-rate change. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Tile t set t.ratedJoulesPerStep = :joules where t.tileType = :type")
    int updateRatedJoulesByType(@Param("type") TileType type, @Param("joules") BigDecimal joules);
}
