package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.SimulationSpeedRequest;
import com.backend.javaleap.model.response.SimulationStatusResponse;
import com.backend.javaleap.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Footfall simulator controls (demo System actor). */
@RestController
@RequestMapping(AppConstants.API_BASE + "/simulation")
@RequiredArgsConstructor
@Tag(name = "Simulation", description = "Live footfall simulator for demos")
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/status")
    @Operation(summary = "Simulator state and totals")
    public ApiResponse<SimulationStatusResponse> status() {
        return ApiResponse.ok(simulationService.status());
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start generating footfall")
    public ApiResponse<SimulationStatusResponse> start() {
        return ApiResponse.ok("Simulator started", simulationService.start());
    }

    @PostMapping("/stop")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stop the simulator")
    public ApiResponse<SimulationStatusResponse> stop() {
        return ApiResponse.ok("Simulator stopped", simulationService.stop());
    }

    @PostMapping("/speed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set speed multiplier (1-120)")
    public ApiResponse<SimulationStatusResponse> speed(@Valid @RequestBody SimulationSpeedRequest request) {
        return ApiResponse.ok("Speed set to " + request.speed() + "x", simulationService.setSpeed(request.speed()));
    }

    @PostMapping("/rush-hour")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Triple footfall for a few minutes")
    public ApiResponse<SimulationStatusResponse> rushHour(@RequestParam(defaultValue = "2") int minutes) {
        return ApiResponse.ok("Rush hour started", simulationService.rushHour(minutes));
    }

    @PostMapping("/inject-fault")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Break a random tile (demo of fault alerts)")
    public ApiResponse<SimulationStatusResponse> injectFault() {
        return ApiResponse.ok("Fault injected", simulationService.injectFault());
    }
}
