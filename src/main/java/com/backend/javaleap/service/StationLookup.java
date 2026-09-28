package com.backend.javaleap.service;

import com.backend.javaleap.entity.Station;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Resolves a station by code, or the default (first) station when no code is given. */
@Component
@RequiredArgsConstructor
public class StationLookup {

    private final StationRepository stationRepository;

    public Station resolve(String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            return stationRepository.findAllByOrderByNameAsc().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No station configured"));
        }
        String code = stationCode.trim().toUpperCase(Locale.ROOT);
        return stationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + code));
    }
}
