package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.TileStatus;
import com.backend.javaleap.entity.enums.TileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One kinetic energy harvesting floor tile - an IoT node in the network. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "tile")
public class Tile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    /** Human readable id, e.g. CHC-PF1-R2C07. */
    @Column(name = "tile_code", nullable = false, unique = true, length = 30)
    private String tileCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tile_type", nullable = false, length = 20)
    private TileType tileType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TileStatus status = TileStatus.ACTIVE;

    @Column(name = "grid_row", nullable = false)
    private Integer gridRow;

    @Column(name = "grid_col", nullable = false)
    private Integer gridCol;

    /** Tile centre X on the floor-map canvas. */
    @Column(name = "pos_x", nullable = false, precision = 8, scale = 2)
    private BigDecimal posX;

    /** Tile centre Y on the floor-map canvas. */
    @Column(name = "pos_y", nullable = false, precision = 8, scale = 2)
    private BigDecimal posY;

    @Column(name = "rated_joules_per_step", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratedJoulesPerStep;

    @Builder.Default
    @Column(name = "health_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal healthPct = BigDecimal.valueOf(100);

    @Builder.Default
    @Column(name = "steps_today", nullable = false)
    private Long stepsToday = 0L;

    @Builder.Default
    @Column(name = "energy_today_j", nullable = false, precision = 14, scale = 3)
    private BigDecimal energyTodayJ = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_steps", nullable = false)
    private Long totalSteps = 0L;

    @Builder.Default
    @Column(name = "total_energy_j", nullable = false, precision = 18, scale = 3)
    private BigDecimal totalEnergyJ = BigDecimal.ZERO;

    @Column(name = "installed_on", nullable = false)
    private LocalDate installedOn;

    @Column(name = "last_maintenance_on")
    private LocalDate lastMaintenanceOn;

    @Column(name = "last_step_at")
    private LocalDateTime lastStepAt;

    @Version
    @Column(nullable = false)
    private Long version;

    // ------------------------------------------------------------------ domain logic

    /** A tile can harvest only when it is ACTIVE or IDLE. */
    public boolean isOperational() {
        return status == TileStatus.ACTIVE || status == TileStatus.IDLE;
    }

    /**
     * Records footsteps on this tile and adds the harvested energy to all counters.
     *
     * @param steps  number of steps in this interval
     * @param joules energy harvested in this interval
     * @param at     time of the last step
     */
    public void registerSteps(int steps, BigDecimal joules, LocalDateTime at) {
        if (steps <= 0) {
            return;
        }
        BigDecimal j = joules.setScale(3, RoundingMode.HALF_UP);
        this.stepsToday += steps;
        this.totalSteps += steps;
        this.energyTodayJ = this.energyTodayJ.add(j);
        this.totalEnergyJ = this.totalEnergyJ.add(j);
        this.lastStepAt = at;
        if (this.status == TileStatus.IDLE) {
            this.status = TileStatus.ACTIVE;
        }
    }

    /** Clears the daily counters (called at midnight). */
    public void resetDailyCounters() {
        this.stepsToday = 0L;
        this.energyTodayJ = BigDecimal.ZERO;
    }
}
