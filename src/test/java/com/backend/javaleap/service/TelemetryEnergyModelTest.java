package com.backend.javaleap.service;

import com.backend.javaleap.entity.Battery;
import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.Tile;
import com.backend.javaleap.entity.enums.BatteryStatus;
import com.backend.javaleap.entity.enums.TileType;
import com.backend.javaleap.model.AppProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests of the energy formulas (no Spring). */
class TelemetryEnergyModelTest {

    private static final AppProperties.Kinetic CFG = new AppProperties.Kinetic(0.95, 80, 85, 40, 24, 90);

    private static KineticConversionRate rate(String j, String eff) {
        return KineticConversionRate.builder().tileType(TileType.PIEZO)
                .joulesPerStep(new BigDecimal(j)).conversionEfficiency(new BigDecimal(eff)).build();
    }

    @Test
    void energyIsStepsTimesRatedTimesEfficiencyTimesHealth() {
        Tile tile = Tile.builder().ratedJoulesPerStep(new BigDecimal("2.50")).healthPct(new BigDecimal("80")).build();
        // 100 x 2.5 x 0.9 x 0.8 = 180 J
        assertThat(TelemetryProcessor.energyFor(tile, 100, rate("2.50", "0.900"))).isEqualByComparingTo("180.000");
    }

    @Test
    void distributeKeepsEveryStep() {
        List<Tile> tiles = List.of(Tile.builder().tileCode("a").build(), Tile.builder().tileCode("b").build(),
                Tile.builder().tileCode("c").build());
        Map<Tile, Long> map = TelemetryProcessor.distribute(10, tiles);
        assertThat(map.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(10);
        assertThat(map.values()).allMatch(v -> v == 3 || v == 4);

        Map<Tile, Long> few = TelemetryProcessor.distribute(2, tiles);
        assertThat(few).hasSize(2);
    }

    @Test
    void routingChargesBatteryUpToThresholdThenExports() {
        Battery b = Battery.builder().capacityWh(new BigDecimal("1000")).chargeWh(new BigDecimal("790"))
                .maxChargeRateW(BigDecimal.TEN).build();
        // room to 80% = 10 Wh stored -> needs 10/0.95 = 10.526 Wh input; 20 Wh harvested
        BigDecimal[] r = TelemetryProcessor.route(new BigDecimal("20"), List.of(b), CFG);
        assertThat(b.getChargeWh()).isEqualByComparingTo("800.000");
        assertThat(r[0].doubleValue()).isCloseTo(10.526, org.assertj.core.data.Offset.offset(0.001));
        assertThat(r[1].doubleValue()).isCloseTo(9.474, org.assertj.core.data.Offset.offset(0.001));
        assertThat(b.getStatus()).isEqualTo(BatteryStatus.CHARGING);

        BigDecimal[] full = TelemetryProcessor.route(new BigDecimal("5"), List.of(b), CFG);
        assertThat(full[0]).isEqualByComparingTo("0");
        assertThat(full[1]).isEqualByComparingTo("5");
        assertThat(b.getStatus()).isEqualTo(BatteryStatus.IDLE);
    }
}
