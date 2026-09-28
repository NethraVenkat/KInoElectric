package com.backend.javaleap.service;

import com.backend.javaleap.entity.ApiClient;
import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.repository.ApiClientRepository;
import com.backend.javaleap.repository.BatteryRepository;
import com.backend.javaleap.repository.KineticConversionRateRepository;
import com.backend.javaleap.repository.StationRepository;
import com.backend.javaleap.repository.TileRepository;
import com.backend.javaleap.repository.UserRepository;
import com.backend.javaleap.repository.ZoneRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture: station TST with sectors ENT (2x5 piezo tiles) and PF1 (2x5, one electromagnetic),
 * a 2 kWh battery at 50%, conversion rates, 3 users and an API key.
 */
public class KineticTestData {

    public static final String API_KEY = "pdk_sys_TestGatewayKey12345";
    public static final String PASSWORD = "Secret@123";

    public final Station station;
    public final Zone entry;
    public final Zone platform;
    public final Battery battery;
    public final User admin;
    public final User accountant;
    public final User system;

    public KineticTestData(ApplicationContext ctx) {
        StationRepository stations = ctx.getBean(StationRepository.class);
        ZoneRepository zones = ctx.getBean(ZoneRepository.class);
        TileRepository tiles = ctx.getBean(TileRepository.class);
        BatteryRepository batteries = ctx.getBean(BatteryRepository.class);
        KineticConversionRateRepository rates = ctx.getBean(KineticConversionRateRepository.class);
        UserRepository users = ctx.getBean(UserRepository.class);
        ApiClientRepository apiClients = ctx.getBean(ApiClientRepository.class);
        PasswordEncoder encoder = ctx.getBean(PasswordEncoder.class);

        rates.save(KineticConversionRate.builder().tileType(TileType.PIEZO)
                .joulesPerStep(new BigDecimal("2.50")).conversionEfficiency(new BigDecimal("0.900")).build());
        rates.save(KineticConversionRate.builder().tileType(TileType.ELECTROMAGNETIC)
                .joulesPerStep(new BigDecimal("4.00")).conversionEfficiency(new BigDecimal("0.920")).build());

        station = stations.save(Station.builder().code("TST").name("Test Station").city("Chennai")
                .dailyFootfallEstimate(100_000).build());
        entry = zones.save(zone("ENT", ZoneType.ENTRY, 100, 1));
        platform = zones.save(zone("PF1", ZoneType.PLATFORM, 200, 2));

        List<Tile> all = new ArrayList<>();
        all.addAll(grid(entry, false));
        all.addAll(grid(platform, true));
        tiles.saveAll(all);

        battery = batteries.save(Battery.builder().station(station).code("TST-BAT").name("Bank")
                .capacityWh(new BigDecimal("2000")).chargeWh(new BigDecimal("1000"))
                .maxChargeRateW(new BigDecimal("500")).build());

        admin = users.save(user("admin", Role.ADMIN, encoder));
        accountant = users.save(user("accountant", Role.ACCOUNTANT, encoder));
        system = users.save(user("system", Role.SYSTEM, encoder));

        apiClients.save(ApiClient.builder().name("Test gateway").keyPrefix(ApiKeyService.PREFIX)
                .keyHash(ApiKeyService.sha256(API_KEY)).user(system).enabled(true).build());
    }

    private Zone zone(String code, ZoneType type, int capacity, int order) {
        return Zone.builder().station(station).code(code).name(code + " sector").zoneType(type)
                .trafficWeight(BigDecimal.ONE).maxCapacity(capacity)
                .posX(20).posY(20 + order * 200).width(300).height(150)
                .gridRows(2).gridCols(5).displayOrder(order).build();
    }

    private static List<Tile> grid(Zone zone, boolean firstIsElectromagnetic) {
        List<Tile> list = new ArrayList<>();
        for (int r = 1; r <= 2; r++) {
            for (int c = 1; c <= 5; c++) {
                boolean em = firstIsElectromagnetic && r == 1 && c == 1;
                list.add(Tile.builder().zone(zone)
                        .tileCode(String.format("TST-%s-R%dC%02d", zone.getCode(), r, c))
                        .tileType(em ? TileType.ELECTROMAGNETIC : TileType.PIEZO)
                        .status(TileStatus.ACTIVE).gridRow(r).gridCol(c)
                        .posX(BigDecimal.TEN).posY(BigDecimal.TEN)
                        .ratedJoulesPerStep(new BigDecimal(em ? "4.00" : "2.50"))
                        .healthPct(new BigDecimal("100")).installedOn(LocalDate.now()).build());
            }
        }
        return list;
    }

    private static User user(String username, Role role, PasswordEncoder encoder) {
        return User.builder().username(username).email(username + "@test.in")
                .passwordHash(encoder.encode(PASSWORD)).fullName(username).role(role).enabled(true).build();
    }
}
