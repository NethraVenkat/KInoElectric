package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.TilePattern;
import com.backend.javaleap.model.request.SectorCreateRequest;
import com.backend.javaleap.model.request.SectorUpdateRequest;
import com.backend.javaleap.model.request.TileStatusUpdateRequest;
import com.backend.javaleap.model.response.SectorDetailResponse;
import com.backend.javaleap.model.response.SectorResponse;
import com.backend.javaleap.model.response.TileDetailResponse;
import com.backend.javaleap.model.response.TileResponse;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.ZoneRepository;
import com.backend.javaleap.repository.projection.ZoneStatsView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Floor tile sectors (a sector = a zone of a station fitted with kinetic tiles).
 * Creating a sector also creates its tile grid and its analytic account ("Transit Station Zone").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectorService {

    private static final int CANVAS_MARGIN = 20;
    private static final int CELL = 44;

    private final StationLookup stationLookup;
    private final ZoneRepository zoneRepository;
    private final TileRepository tileRepository;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final UserRepository userRepository;
    private final ConversionRateService conversionRateService;
    private final AlertService alertService;
    private final KineticMapper kineticMapper;
    private final TileMapper tileMapper;

    // ------------------------------------------------------------------ create

    @Transactional
    public SectorDetailResponse create(SectorCreateRequest req) {
        Station station = stationLookup.resolve(req.stationCode());
        String code = req.code().trim();

        if (zoneRepository.findByStationIdAndCode(station.getId(), code).isPresent()) {
            throw new ConflictException("Sector " + code + " already exists in station " + station.getCode());
        }
        String accountCode = "AA-" + station.getCode() + "-" + code;
        if (analyticAccountRepository.existsByCode(accountCode)) {
            throw new ConflictException("Analytic account " + accountCode + " already exists");
        }

        List<Zone> existing = zoneRepository.findByStationIdOrderByDisplayOrderAsc(station.getId());
        Zone zone = Zone.builder()
                .station(station)
                .code(code)
                .name(req.name().trim())
                .zoneType(req.zoneType())
                .trafficWeight(req.trafficWeight() == null ? BigDecimal.ONE
                        : req.trafficWeight().setScale(2, RoundingMode.HALF_UP))
                .maxCapacity(req.maxCapacity())
                .gridRows(req.gridRows())
                .gridCols(req.gridCols())
                .displayOrder(existing.stream().mapToInt(Zone::getDisplayOrder).max().orElse(0) + 1)
                .build();
        placeOnCanvas(zone, req, station, existing);
        zone = zoneRepository.save(zone);

        List<Tile> tiles = buildTiles(station, zone, req.tilePattern() == null ? TilePattern.MIXED : req.tilePattern());
        tileRepository.saveAll(tiles);

        AnalyticAccount account = analyticAccountRepository.save(AnalyticAccount.builder()
                .code(accountCode)
                .name(station.getName() + " - " + zone.getName())
                .station(station)
                .zone(zone)
                .description("Transit Station Zone: " + zone.getName())
                .active(true)
                .build());

        log.info("Sector {} created in {} with {} tiles, analytic account {}",
                code, station.getCode(), tiles.size(), account.getCode());
        return detail(zone, account, tiles);
    }

    /** Uses the given layout, or stacks the sector below the existing ones and grows the canvas. */
    private void placeOnCanvas(Zone zone, SectorCreateRequest req, Station station, List<Zone> existing) {
        if (req.hasLayout()) {
            zone.setPosX(req.posX());
            zone.setPosY(req.posY());
            zone.setWidth(req.width());
            zone.setHeight(req.height());
        } else {
            int bottom = existing.stream().mapToInt(z -> z.getPosY() + z.getHeight()).max().orElse(0);
            zone.setPosX(CANVAS_MARGIN);
            zone.setPosY(bottom + CANVAS_MARGIN);
            zone.setWidth(Math.min(station.getLayoutWidth() - 2 * CANVAS_MARGIN, req.gridCols() * CELL + 20));
            zone.setHeight(req.gridRows() * CELL + 40);
        }
        int needed = zone.getPosY() + zone.getHeight() + CANVAS_MARGIN;
        if (needed > station.getLayoutHeight()) {
            station.setLayoutHeight(needed);
        }
        int neededWidth = zone.getPosX() + zone.getWidth() + CANVAS_MARGIN;
        if (neededWidth > station.getLayoutWidth()) {
            station.setLayoutWidth(neededWidth);
        }
    }

    private List<Tile> buildTiles(Station station, Zone zone, TilePattern pattern) {
        Map<TileType, KineticConversionRate> rates = conversionRateService.rates();
        BigDecimal cellW = BigDecimal.valueOf(zone.getWidth() - 20L)
                .divide(BigDecimal.valueOf(zone.getGridCols()), 6, RoundingMode.HALF_UP);
        BigDecimal cellH = BigDecimal.valueOf(zone.getHeight() - 40L)
                .divide(BigDecimal.valueOf(zone.getGridRows()), 6, RoundingMode.HALF_UP);
        LocalDate today = LocalDate.now();
        List<Tile> tiles = new ArrayList<>();
        for (int r = 1; r <= zone.getGridRows(); r++) {
            for (int c = 1; c <= zone.getGridCols(); c++) {
                TileType type = switch (pattern) {
                    case ALL_PIEZO -> TileType.PIEZO;
                    case ALL_ELECTROMAGNETIC -> TileType.ELECTROMAGNETIC;
                    case MIXED -> (r + c) % 3 == 0 ? TileType.ELECTROMAGNETIC : TileType.PIEZO;
                };
                tiles.add(Tile.builder()
                        .zone(zone)
                        .tileCode(String.format("%s-%s-R%dC%02d", station.getCode(), zone.getCode(), r, c))
                        .tileType(type)
                        .status(TileStatus.ACTIVE)
                        .gridRow(r)
                        .gridCol(c)
                        .posX(BigDecimal.valueOf(zone.getPosX() + 10L)
                                .add(cellW.multiply(BigDecimal.valueOf(c - 0.5))).setScale(2, RoundingMode.HALF_UP))
                        .posY(BigDecimal.valueOf(zone.getPosY() + 30L)
                                .add(cellH.multiply(BigDecimal.valueOf(r - 0.5))).setScale(2, RoundingMode.HALF_UP))
                        .ratedJoulesPerStep(rates.get(type).getJoulesPerStep())
                        .healthPct(BigDecimal.valueOf(100))
                        .installedOn(today)
                        .build());
            }
        }
        return tiles;
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public List<SectorResponse> list(String stationCode) {
        Station station = stationLookup.resolve(stationCode);
        Map<Long, ZoneStatsView> stats = statsByZone(station.getId());
        Map<Long, AnalyticAccount> accounts = analyticAccountRepository.findByStationIdOrderByCodeAsc(station.getId())
                .stream().filter(a -> a.getZone() != null)
                .collect(Collectors.toMap(a -> a.getZone().getId(), Function.identity(), (a, b) -> a));
        return zoneRepository.findByStationIdOrderByDisplayOrderAsc(station.getId()).stream()
                .map(z -> kineticMapper.toResponse(z, stats.get(z.getId()), accounts.get(z.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SectorDetailResponse get(Long id) {
        Zone zone = findZone(id);
        return detail(zone, analyticAccountRepository.findByZoneId(id).orElse(null),
                tileRepository.findByZoneIdOrderByGridRowAscGridColAsc(id));
    }

    @Transactional(readOnly = true)
    public List<TileResponse> tiles(Long id) {
        findZone(id);
        return tileRepository.findByZoneIdOrderByGridRowAscGridColAsc(id).stream().map(tileMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TileDetailResponse tile(Long tileId) {
        return tileMapper.toDetail(tileRepository.findWithZoneById(tileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tile", tileId)));
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public SectorResponse update(Long id, SectorUpdateRequest req) {
        Zone zone = findZone(id);
        if (req.name() != null && !req.name().isBlank()) {
            zone.setName(req.name().trim());
            analyticAccountRepository.findByZoneId(id)
                    .ifPresent(a -> a.setName(zone.getStation().getName() + " - " + zone.getName()));
        }
        if (req.trafficWeight() != null) {
            zone.setTrafficWeight(req.trafficWeight().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.maxCapacity() != null) {
            zone.setMaxCapacity(req.maxCapacity());
        }
        return kineticMapper.toResponse(zone, statsByZone(zone.getStation().getId()).get(id),
                analyticAccountRepository.findByZoneId(id).orElse(null));
    }

    /**
     * Change a tile's status. Returning a tile to ACTIVE from FAULTY/MAINTENANCE counts as a
     * repair: health is restored to 100%, maintenance date set and its alerts resolved.
     */
    @Transactional
    public TileDetailResponse updateTileStatus(Long tileId, TileStatusUpdateRequest req) {
        Tile tile = tileRepository.findWithZoneById(tileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tile", tileId));
        TileStatus previous = tile.getStatus();
        tile.setStatus(req.status());
        if (req.status() == TileStatus.ACTIVE
                && (previous == TileStatus.FAULTY || previous == TileStatus.MAINTENANCE)) {
            tile.setHealthPct(BigDecimal.valueOf(100));
            tile.setLastMaintenanceOn(LocalDate.now());
            alertService.resolveTileAlerts(tile,
                    SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null));
        }
        log.info("Tile {} status {} -> {}{}", tile.getTileCode(), previous, req.status(),
                req.note() == null ? "" : " (" + req.note() + ")");
        return tileMapper.toDetail(tile);
    }

    // ------------------------------------------------------------------ helpers

    private SectorDetailResponse detail(Zone zone, AnalyticAccount account, List<Tile> tiles) {
        ZoneStatsView stats = statsByZone(zone.getStation().getId()).get(zone.getId());
        return new SectorDetailResponse(kineticMapper.toResponse(zone, stats, account),
                tiles.stream().map(tileMapper::toResponse).toList());
    }

    private Map<Long, ZoneStatsView> statsByZone(Long stationId) {
        return zoneRepository.findZoneStats(stationId).stream()
                .collect(Collectors.toMap(ZoneStatsView::getZoneId, Function.identity()));
    }

    private Zone findZone(Long id) {
        return zoneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sector", id));
    }
}
