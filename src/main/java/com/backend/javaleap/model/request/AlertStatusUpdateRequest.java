package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.AlertStatus;

import jakarta.validation.constraints.NotNull;

/** Acknowledge or resolve an alert. */
public record AlertStatusUpdateRequest(
        @NotNull(message = "status is required") AlertStatus status) {
}
