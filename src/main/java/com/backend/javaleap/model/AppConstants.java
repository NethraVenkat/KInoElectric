package com.backend.javaleap.model;

/** Application-wide constants and unit conversions. */
public final class AppConstants {

    private AppConstants() { }

    public static final String API_BASE = "/api";
    public static final String APP_NAME = "PadaShakti";
    public static final String APP_VERSION = "1.0.0";

    /** 1 kWh = 3,600,000 J */
    public static final double JOULES_PER_KWH = 3_600_000d;
    /** 1 Wh = 3,600 J */
    public static final double JOULES_PER_WH = 3_600d;

    public static double joulesToKwh(double joules) {
        return joules / JOULES_PER_KWH;
    }

    public static double joulesToWh(double joules) {
        return joules / JOULES_PER_WH;
    }
}
