package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.TileStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Change a tile's status (e.g. send to MAINTENANCE, mark ACTIVE after repair). */
public record TileStatusUpdateRequest(
        @NotNull(message = "status is required") TileStatus status,
        @Size(max = 200, message = "note must be at most 200 characters") String note) {
}
