package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.ZoneType;

import java.math.BigDecimal;

/** Zone definition incl. its rectangle on the floor map. */
public record ZoneResponse(
        Long id,
        String code,
        String name,
        ZoneType zoneType,
        BigDecimal trafficWeight,
        Integer maxCapacity,
        Integer posX,
        Integer posY,
        Integer width,
        Integer height,
        Integer gridRows,
        Integer gridCols,
        Integer displayOrder) {
}
