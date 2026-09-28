package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.SectorCreateRequest;
import com.backend.javaleap.model.request.SectorUpdateRequest;
import com.backend.javaleap.model.request.TileStatusUpdateRequest;
import com.backend.javaleap.model.response.SectorDetailResponse;
import com.backend.javaleap.model.response.SectorResponse;
import com.backend.javaleap.model.response.TileDetailResponse;
import com.backend.javaleap.model.response.TileResponse;
import com.backend.javaleap.service.SectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Floor tile sectors and their tiles. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/kinetic")
@RequiredArgsConstructor
@Tag(name = "Kinetic - Sectors", description = "Floor tile sectors, tiles and analytic accounts")
public class SectorController {

    private final SectorService sectorService;

    @PostMapping("/sectors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a floor tile sector (creates its tile grid and analytic account)")
    public ApiResponse<SectorDetailResponse> create(@Valid @RequestBody SectorCreateRequest request) {
        return ApiResponse.ok("Sector created", sectorService.create(request));
    }

    @GetMapping("/sectors")
    @Operation(summary = "List sectors of a station with live totals")
    public ApiResponse<List<SectorResponse>> list(@RequestParam(required = false) String stationCode) {
        return ApiResponse.ok(sectorService.list(stationCode));
    }

    @GetMapping("/sectors/{id}")
    @Operation(summary = "Sector details with all tiles")
    public ApiResponse<SectorDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(sectorService.get(id));
    }

    @PatchMapping("/sectors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update sector name, traffic weight or capacity")
    public ApiResponse<SectorResponse> update(@PathVariable Long id, @Valid @RequestBody SectorUpdateRequest request) {
        return ApiResponse.ok("Sector updated", sectorService.update(id, request));
    }

    @GetMapping("/sectors/{id}/tiles")
    @Operation(summary = "Tiles of a sector")
    public ApiResponse<List<TileResponse>> tiles(@PathVariable Long id) {
        return ApiResponse.ok(sectorService.tiles(id));
    }

    @GetMapping("/tiles/{tileId}")
    @Operation(summary = "Tile details")
    public ApiResponse<TileDetailResponse> tile(@PathVariable Long tileId) {
        return ApiResponse.ok(sectorService.tile(tileId));
    }

    @PatchMapping("/tiles/{tileId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change tile status (ACTIVE after repair restores health and resolves alerts)")
    public ApiResponse<TileDetailResponse> updateTileStatus(@PathVariable Long tileId,
                                                            @Valid @RequestBody TileStatusUpdateRequest request) {
        return ApiResponse.ok("Tile status updated", sectorService.updateTileStatus(tileId, request));
    }
}
