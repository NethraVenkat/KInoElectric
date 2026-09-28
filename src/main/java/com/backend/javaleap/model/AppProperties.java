package com.backend.javaleap.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Type-safe binding for all custom {@code app.*} settings in application.properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Jwt jwt, Energy energy, Simulation simulation, Kinetic kinetic) {

    /** Allowed frontend origins for CORS. */
    public record Cors(List<String> allowedOrigins) { }

    /** JWT signing secret (Base64) and token lifetime. */
    public record Jwt(String secret, long expirationMs) { }

    /** Energy and impact formula constants. */
    public record Energy(double minJoulesPerStep,
                         double maxJoulesPerStep,
                         double tariffPerKwh,
                         double co2KgPerKwh) { }

    /** Footfall simulator: auto-start flag and tick interval. */
    public record Simulation(boolean enabled, long tickMs) { }

    /**
     * Kinetic network behaviour.
     *
     * @param batteryChargeEfficiency fraction of routed energy that ends up stored (0-1)
     * @param exportThresholdPct      batteries are charged up to this %, surplus is exported to the grid
     * @param overcrowdingDensityPct  zone density that raises an overcrowding alert
     * @param degradedHealthPct       tile health below which a degradation alert is raised
     * @param readingRetentionHours   how long raw readings are kept
     * @param telemetryRetentionDays  how long telemetry batch audit rows are kept
     */
    public record Kinetic(double batteryChargeEfficiency,
                          double exportThresholdPct,
                          double overcrowdingDensityPct,
                          double degradedHealthPct,
                          int readingRetentionHours,
                          int telemetryRetentionDays) { }
}
