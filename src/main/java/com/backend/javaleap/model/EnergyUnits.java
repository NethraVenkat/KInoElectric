package com.backend.javaleap.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Precise BigDecimal energy/unit conversions used by mappers and services. */
public final class EnergyUnits {

    private static final BigDecimal J_PER_WH = BigDecimal.valueOf(3_600);
    private static final BigDecimal J_PER_KWH = BigDecimal.valueOf(3_600_000);

    private EnergyUnits() { }

    /** Joules to watt-hours, 3 decimals. */
    public static BigDecimal toWh(BigDecimal joules) {
        return joules == null ? BigDecimal.ZERO : joules.divide(J_PER_WH, 3, RoundingMode.HALF_UP);
    }

    /** Joules to kilowatt-hours, 6 decimals. */
    public static BigDecimal toKwh(BigDecimal joules) {
        return joules == null ? BigDecimal.ZERO : joules.divide(J_PER_KWH, 6, RoundingMode.HALF_UP);
    }

    /** Watt-hours to joules. */
    public static BigDecimal whToJoules(BigDecimal wh) {
        return wh == null ? BigDecimal.ZERO : wh.multiply(J_PER_WH);
    }

    /** Rounds a nullable double (e.g. SQL avg) to 2 decimals. */
    public static BigDecimal round2(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
