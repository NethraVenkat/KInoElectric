package com.backend.javaleap.controller;

import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.ConversionRateUpdateRequest;
import com.backend.javaleap.model.response.ConversionRateResponse;
import com.backend.javaleap.service.ConversionRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Kinetic conversion rates (Transit Energy Director). */
@RestController
@RequestMapping(AppConstants.API_BASE + "/kinetic/conversion-rates")
@RequiredArgsConstructor
@Tag(name = "Kinetic - Conversion rates", description = "Joules per step and conversion efficiency per tile type")
public class ConversionRateController {

    private final ConversionRateService conversionRateService;

    @GetMapping
    @Operation(summary = "Current conversion rates")
    public ApiResponse<List<ConversionRateResponse>> list() {
        return ApiResponse.ok(conversionRateService.list());
    }

    @PutMapping("/{tileType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set the conversion rate of a tile type (optionally re-rate existing tiles)")
    public ApiResponse<ConversionRateResponse> update(@PathVariable TileType tileType,
                                                      @Valid @RequestBody ConversionRateUpdateRequest request) {
        return ApiResponse.ok("Conversion rate updated", conversionRateService.update(tileType, request));
    }
}
