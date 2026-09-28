package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.StationStatus;

import java.math.BigDecimal;

/** Station details for the station selector and floor-map canvas size. */
public record StationResponse(
        Long id,
        String code,
        String name,
        String city,
        String lineName,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer dailyFootfallEstimate,
        Integer layoutWidth,
        Integer layoutHeight,
        StationStatus status) {
}
