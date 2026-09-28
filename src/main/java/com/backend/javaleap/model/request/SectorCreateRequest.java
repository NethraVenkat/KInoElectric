package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.model.TilePattern;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * POST /api/kinetic/sectors - add a floor tile sector to a station.
 * Layout (posX, posY, width, height) is optional; if omitted the sector is placed automatically.
 */
public record SectorCreateRequest(
        @NotBlank(message = "stationCode is required") @Size(max = 20) String stationCode,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "code must be 2-10 uppercase letters/digits, e.g. CON2")
        String code,

        @NotBlank(message = "name is required") @Size(max = 80) String name,

        @NotNull(message = "zoneType is required") ZoneType zoneType,

        @DecimalMin(value = "0.10", message = "trafficWeight must be at least 0.10")
        @DecimalMax(value = "5.00", message = "trafficWeight must be at most 5.00")
        BigDecimal trafficWeight,

        @NotNull(message = "maxCapacity is required")
        @Min(value = 10, message = "maxCapacity must be at least 10")
        @Max(value = 10000, message = "maxCapacity must be at most 10000")
        Integer maxCapacity,

        @NotNull @Min(value = 1, message = "gridRows must be 1-10") @Max(value = 10, message = "gridRows must be 1-10")
        Integer gridRows,

        @NotNull @Min(value = 1, message = "gridCols must be 1-30") @Max(value = 30, message = "gridCols must be 1-30")
        Integer gridCols,

        TilePattern tilePattern,

        @Min(0) Integer posX,
        @Min(0) Integer posY,
        @Min(40) Integer width,
        @Min(40) Integer height) {

    public boolean hasLayout() {
        return posX != null && posY != null && width != null && height != null;
    }
}
