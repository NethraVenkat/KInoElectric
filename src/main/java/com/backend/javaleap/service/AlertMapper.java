package com.backend.javaleap.service;

import com.backend.javaleap.entity.Alert;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.model.response.AlertResponse;

import org.springframework.stereotype.Component;

/** Alert entity -> DTO. Zone, tile and users are optional. */
@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert a) {
        if (a == null) {
            return null;
        }
        return new AlertResponse(a.getId(), a.getAlertType(), a.getSeverity(), a.getTitle(), a.getMessage(),
                a.getStatus(),
                a.getZone() != null ? a.getZone().getId() : null,
                a.getZone() != null ? a.getZone().getCode() : null,
                a.getTile() != null ? a.getTile().getId() : null,
                a.getTile() != null ? a.getTile().getTileCode() : null,
                a.getCreatedAt(),
                a.getAcknowledgedAt(), username(a.getAcknowledgedBy()),
                a.getResolvedAt(), username(a.getResolvedBy()));
    }

    private static String username(User u) {
        return u == null ? null : u.getUsername();
    }
}
