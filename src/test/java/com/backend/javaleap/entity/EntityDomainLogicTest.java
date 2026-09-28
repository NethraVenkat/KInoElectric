package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.AlertStatus;
import com.backend.javaleap.entity.enums.TileStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for the business methods on entities (no Spring, no DB). */
class EntityDomainLogicTest {

    @Test
    void tileRegisterStepsUpdatesAllCountersAndWakesIdleTile() {
        Tile tile = Tile.builder().status(TileStatus.IDLE).build();
        LocalDateTime now = LocalDateTime.now();

        tile.registerSteps(3, new BigDecimal("9.6"), now);

        assertThat(tile.getStepsToday()).isEqualTo(3L);
        assertThat(tile.getTotalSteps()).isEqualTo(3L);
        assertThat(tile.getEnergyTodayJ()).isEqualByComparingTo("9.600");
        assertThat(tile.getTotalEnergyJ()).isEqualByComparingTo("9.600");
        assertThat(tile.getLastStepAt()).isEqualTo(now);
        assertThat(tile.getStatus()).isEqualTo(TileStatus.ACTIVE);
    }

    @Test
    void tileIgnoresZeroStepsAndResetsDaily() {
        Tile tile = Tile.builder().build();
        tile.registerSteps(0, BigDecimal.TEN, LocalDateTime.now());
        assertThat(tile.getStepsToday()).isZero();

        tile.registerSteps(5, BigDecimal.TEN, LocalDateTime.now());
        tile.resetDailyCounters();
        assertThat(tile.getStepsToday()).isZero();
        assertThat(tile.getEnergyTodayJ()).isEqualByComparingTo("0");
        assertThat(tile.getTotalSteps()).isEqualTo(5L);
    }

    @Test
    void faultyTileIsNotOperational() {
        assertThat(Tile.builder().status(TileStatus.FAULTY).build().isOperational()).isFalse();
        assertThat(Tile.builder().status(TileStatus.ACTIVE).build().isOperational()).isTrue();
    }

    @Test
    void batteryChargeIsClampedAndPercentComputed() {
        Battery b = Battery.builder()
                .capacityWh(new BigDecimal("2000"))
                .chargeWh(new BigDecimal("1990"))
                .build();

        BigDecimal applied = b.applyEnergyWh(new BigDecimal("50"));
        assertThat(applied).isEqualByComparingTo("10");
        assertThat(b.getChargePct()).isEqualByComparingTo("100.00");

        b.applyEnergyWh(new BigDecimal("-5000"));
        assertThat(b.getChargeWh()).isEqualByComparingTo("0");
    }

    @Test
    void alertWorkflowOpenToAcknowledgedToResolved() {
        User op = User.builder().username("operator").build();
        Alert a = Alert.builder().build();

        a.acknowledge(op);
        assertThat(a.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(a.getAcknowledgedBy()).isSameAs(op);

        a.resolve(op);
        assertThat(a.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(a.isOpen()).isFalse();
        assertThat(a.getResolvedAt()).isNotNull();
    }
}
