package com.backend.javaleap.service;

import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.request.ConversionRateUpdateRequest;
import com.backend.javaleap.model.response.ConversionRateResponse;
import com.backend.javaleap.repository.KineticConversionRateRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Kinetic conversion rates per tile technology (configured by the Transit Energy Director). */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionRateService {

    private final KineticConversionRateRepository rateRepository;
    private final TileRepository tileRepository;
    private final UserRepository userRepository;
    private final KineticMapper mapper;

    @Transactional(readOnly = true)
    public List<ConversionRateResponse> list() {
        return rateRepository.findAllByOrderByTileTypeAsc().stream()
                .map(r -> mapper.toResponse(r, tileRepository.countByTileType(r.getTileType())))
                .toList();
    }

    @Transactional
    public ConversionRateResponse update(TileType type, ConversionRateUpdateRequest req) {
        KineticConversionRate rate = find(type);
        rate.setJoulesPerStep(req.joulesPerStep().setScale(2, RoundingMode.HALF_UP));
        rate.setConversionEfficiency(req.conversionEfficiency().setScale(3, RoundingMode.HALF_UP));
        if (req.description() != null) {
            rate.setDescription(req.description().trim());
        }
        SecurityUtils.currentUserId().flatMap(userRepository::findById).ifPresent(rate::setUpdatedBy);

        int tilesUpdated = 0;
        if (Boolean.TRUE.equals(req.applyToExistingTiles())) {
            tilesUpdated = tileRepository.updateRatedJoulesByType(type, rate.getJoulesPerStep());
        }
        log.info("Conversion rate {} set to {} J x {} (tiles updated: {})",
                type, rate.getJoulesPerStep(), rate.getConversionEfficiency(), tilesUpdated);
        return mapper.toResponse(rate, tileRepository.countByTileType(type));
    }

    /** Current rates keyed by tile type (used by telemetry and sector creation). */
    @Transactional(readOnly = true)
    public Map<TileType, KineticConversionRate> rates() {
        Map<TileType, KineticConversionRate> map = new EnumMap<>(TileType.class);
        rateRepository.findAll().forEach(r -> map.put(r.getTileType(), r));
        for (TileType t : TileType.values()) {
            if (!map.containsKey(t)) {
                throw new ResourceNotFoundException("Conversion rate missing for tile type " + t);
            }
        }
        return map;
    }

    private KineticConversionRate find(TileType type) {
        return rateRepository.findByTileType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Conversion rate not found for " + type));
    }
}
