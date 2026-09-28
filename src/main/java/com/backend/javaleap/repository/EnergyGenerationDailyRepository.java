package com.backend.javaleap.repository;

import com.backend.javaleap.entity.EnergyGenerationDaily;
import com.backend.javaleap.repository.projection.DailyGenerationView;
import com.backend.javaleap.repository.projection.ZoneExportView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnergyGenerationDailyRepository extends JpaRepository<EnergyGenerationDaily, Long> {

    Optional<EnergyGenerationDaily> findByZoneIdAndGenDate(Long zoneId, LocalDate genDate);

    /** Per-sector daily rows in a date range. */
    @EntityGraph(attributePaths = "zone")
    @Query("""
            select g from EnergyGenerationDaily g
            where g.station.id = :stationId and g.genDate between :from and :to
            order by g.genDate, g.zone.displayOrder
            """)
    List<EnergyGenerationDaily> findSectorRows(@Param("stationId") Long stationId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /** Station totals per day in a date range. */
    @Query("""
            select g.genDate as genDate,
                   sum(g.steps) as steps,
                   sum(g.energyJ) as energyJ,
                   sum(g.storedJ) as storedJ,
                   sum(g.exportedJ) as exportedJ,
                   max(g.peakPowerW) as peakPowerW
            from EnergyGenerationDaily g
            where g.station.id = :stationId and g.genDate between :from and :to
            group by g.genDate
            order by g.genDate
            """)
    List<DailyGenerationView> findStationDaily(@Param("stationId") Long stationId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /** Total energy exported to the grid in a period (used for monthly invoicing). */
    @Query("""
            select coalesce(sum(g.exportedJ), 0)
            from EnergyGenerationDaily g
            where g.station.id = :stationId and g.genDate between :from and :to
            """)
    BigDecimal sumExportedJ(@Param("stationId") Long stationId,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    /** Energy exported to the grid per floor sector in a period (energy sales orders). */
    @Query("""
            select g.zone.id as zoneId, g.zone.code as zoneCode, g.zone.name as zoneName,
                   coalesce(sum(g.exportedJ), 0) as exportedJ
            from EnergyGenerationDaily g
            where g.station.id = :stationId and g.genDate between :from and :to
            group by g.zone.id, g.zone.code, g.zone.name, g.zone.displayOrder
            order by g.zone.displayOrder
            """)
    List<ZoneExportView> sumExportedByZone(@Param("stationId") Long stationId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);
}
