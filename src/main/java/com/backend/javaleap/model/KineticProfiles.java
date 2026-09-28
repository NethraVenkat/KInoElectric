package com.backend.javaleap.model;

import com.backend.javaleap.entity.enums.ZoneType;

/**
 * Engineering assumptions per sector type, shared by telemetry and the simulator.
 * <ul>
 *   <li><b>stepsPerPerson</b> - kinetic tiles a passenger steps on while crossing the sector</li>
 *   <li><b>dwellSeconds</b> - average time a passenger spends in the sector</li>
 * </ul>
 * Occupancy uses Little's Law: people in sector = arrival rate x dwell time.
 */
public final class KineticProfiles {

    private KineticProfiles() { }

    /** Average tile steps per passenger passing through this sector type. */
    public static double stepsPerPerson(ZoneType type) {
        return switch (type) {
            case ENTRY, EXIT, CORRIDOR -> 3.0;
            case STAIRS, CONCOURSE, PLATFORM -> 2.0;
        };
    }

    /** Average seconds a passenger stays in this sector type. */
    public static int dwellSeconds(ZoneType type) {
        return switch (type) {
            case ENTRY, EXIT -> 20;
            case STAIRS -> 30;
            case CORRIDOR -> 45;
            case CONCOURSE -> 90;
            case PLATFORM -> 240;
        };
    }

    /** Share of station passengers that cross this sector type (half enter, half exit). */
    public static double passengerShare(ZoneType type) {
        return switch (type) {
            case ENTRY, EXIT -> 0.5;
            case STAIRS, CONCOURSE, CORRIDOR, PLATFORM -> 1.0;
        };
    }

    /**
     * Metro day curve: relative footfall for each hour 00-23
     * (night low, rush 08-10 and 17-20).
     */
    public static final double[] HOURLY_FOOTFALL_CURVE = {
            0.02, 0.01, 0.01, 0.01, 0.03, 0.10, 0.35, 0.75,
            1.00, 0.90, 0.55, 0.45, 0.50, 0.45, 0.40, 0.45,
            0.60, 0.85, 1.00, 0.80, 0.50, 0.30, 0.15, 0.05
    };

    /** Fraction of the day's passengers travelling in the given hour. */
    public static double hourShare(int hour) {
        double sum = 0;
        for (double v : HOURLY_FOOTFALL_CURVE) {
            sum += v;
        }
        return HOURLY_FOOTFALL_CURVE[hour] / sum;
    }
}
