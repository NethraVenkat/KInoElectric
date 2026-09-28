package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.AlertSeverity;
import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.entity.enums.AlertType;

import java.time.LocalDateTime;

/** Alert for the bell, toast and alerts table. */
public record AlertResponse(
        Long id,
        AlertType alertType,
        AlertSeverity severity,
        String title,
        String message,
        AlertStatus status,
        Long zoneId,
        String zoneCode,
        Long tileId,
        String tileCode,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        String acknowledgedBy,
        LocalDateTime resolvedAt,
        String resolvedBy) {
}
