package com.backend.javaleap.model.response;

import java.util.List;

/** Sector plus all of its tiles (floor map). */
public record SectorDetailResponse(SectorResponse sector, List<TileResponse> tiles) {
}
