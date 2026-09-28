package com.backend.javaleap.service;

import com.backend.javaleap.entity.Alert;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.AlertSeverity;
import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.entity.enums.AlertType;
import com.backend.javaleap.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Raises alerts without duplicates (one unresolved alert per tile/zone/station and type)
 * and auto-resolves them when the condition clears.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    public static final Set<AlertStatus> UNRESOLVED = EnumSet.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alertRepository;

    @Transactional
    public boolean raiseTileAlert(Station station, Tile tile, AlertType type, AlertSeverity severity,
                                  String title, String message) {
        if (alertRepository.existsByTileIdAndAlertTypeAndStatusIn(tile.getId(), type, UNRESOLVED)) {
            return false;
        }
        save(Alert.builder().station(station).zone(tile.getZone()).tile(tile)
                .alertType(type).severity(severity).title(title).message(message).build());
        return true;
    }

    @Transactional
    public boolean raiseZoneAlert(Station station, Zone zone, AlertType type, AlertSeverity severity,
                                  String title, String message) {
        if (alertRepository.existsByStationIdAndZoneIdAndAlertTypeAndStatusIn(station.getId(), zone.getId(), type,
                UNRESOLVED)) {
            return false;
        }
        save(Alert.builder().station(station).zone(zone)
                .alertType(type).severity(severity).title(title).message(message).build());
        return true;
    }

    @Transactional
    public boolean raiseStationAlert(Station station, AlertType type, AlertSeverity severity,
                                     String title, String message) {
        if (alertRepository.existsByStationIdAndAlertTypeAndStatusIn(station.getId(), type, UNRESOLVED)) {
            return false;
        }
        save(Alert.builder().station(station)
                .alertType(type).severity(severity).title(title).message(message).build());
        return true;
    }

    /** Auto-resolve (by the system) when a zone condition has cleared. */
    @Transactional
    public int resolveZoneAlerts(Zone zone, AlertType type) {
        List<Alert> open = alertRepository.findByZoneIdAndAlertTypeAndStatusIn(zone.getId(), type, UNRESOLVED);
        open.forEach(a -> a.resolve(null));
        return open.size();
    }

    /** Resolve all fault/degradation alerts of a repaired tile. */
    @Transactional
    public int resolveTileAlerts(Tile tile, User by) {
        List<Alert> open = alertRepository.findByTileIdAndStatusIn(tile.getId(), UNRESOLVED);
        open.forEach(a -> a.resolve(by));
        return open.size();
    }

    private void save(Alert alert) {
        alertRepository.save(alert);
        log.info("ALERT [{}] {} - {}", alert.getSeverity(), alert.getAlertType(), alert.getTitle());
    }
}
