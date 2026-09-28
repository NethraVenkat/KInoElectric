package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Live state of the footfall simulator. */
public record SimulationStatusResponse(
        boolean running,
        int speed,
        boolean rushHourActive,
        LocalDateTime rushHourEndsAt,
        long tickMs,
        long ticks,
        LocalDateTime lastTickAt,
        long stepsSimulated,
        BigDecimal energySimulatedWh,
        int faultsInjected,
        int currentPassengersPerHour,
        String lastError) {
}
