package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Alert;
import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.entity.enums.AlertType;
import com.backend.javaleap.repository.projection.AlertCountView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @EntityGraph(attributePaths = {"zone", "tile", "acknowledgedBy", "resolvedBy"})
    Page<Alert> findByStationIdOrderByCreatedAtDesc(Long stationId, Pageable pageable);

    @EntityGraph(attributePaths = {"zone", "tile", "acknowledgedBy", "resolvedBy"})
    Page<Alert> findByStationIdAndStatusInOrderByCreatedAtDesc(Long stationId,
                                                               Collection<AlertStatus> statuses,
                                                               Pageable pageable);

    @EntityGraph(attributePaths = {"zone", "tile", "acknowledgedBy", "resolvedBy"})
    Optional<Alert> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"zone", "tile"})
    List<Alert> findTop10ByStationIdOrderByCreatedAtDesc(Long stationId);

    long countByStationIdAndStatusIn(Long stationId, Collection<AlertStatus> statuses);

    /** De-duplication: is there already an unresolved alert of this type for the tile? */
    boolean existsByTileIdAndAlertTypeAndStatusIn(Long tileId, AlertType type, Collection<AlertStatus> statuses);

    /** De-duplication for zone / station level alerts (crowding, battery). */
    boolean existsByStationIdAndZoneIdAndAlertTypeAndStatusIn(Long stationId, Long zoneId,
                                                              AlertType type, Collection<AlertStatus> statuses);

    boolean existsByStationIdAndAlertTypeAndStatusIn(Long stationId, AlertType type,
                                                     Collection<AlertStatus> statuses);

    /** Unresolved alert counts per severity (bell badge, dashboard). */
    @Query("""
            select a.severity as severity, count(a.id) as total
            from Alert a
            where a.station.id = :stationId
              and a.status <> com.backend.javaleap.entity.enums.AlertStatus.RESOLVED
            group by a.severity
            """)
    List<AlertCountView> countOpenBySeverity(@Param("stationId") Long stationId);

    List<Alert> findByZoneIdAndAlertTypeAndStatusIn(Long zoneId, AlertType type, Collection<AlertStatus> statuses);

    List<Alert> findByTileIdAndStatusIn(Long tileId, Collection<AlertStatus> statuses);
}
