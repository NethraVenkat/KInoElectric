package com.backend.javaleap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Total daily generation of one floor sector - basis for grid export invoicing. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "energy_generation_daily")
public class EnergyGenerationDaily extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "gen_date", nullable = false)
    private LocalDate genDate;

    @Builder.Default
    @Column(nullable = false)
    private Long steps = 0L;

    @Builder.Default
    @Column(name = "energy_j", nullable = false, precision = 16, scale = 3)
    private BigDecimal energyJ = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stored_j", nullable = false, precision = 16, scale = 3)
    private BigDecimal storedJ = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "exported_j", nullable = false, precision = 16, scale = 3)
    private BigDecimal exportedJ = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "peak_power_w", nullable = false, precision = 10, scale = 3)
    private BigDecimal peakPowerW = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "telemetry_count", nullable = false)
    private Integer telemetryCount = 0;

    @Version
    @Column(nullable = false)
    private Long version;

    /** Accumulates one telemetry interval into the daily totals. */
    public void accumulate(long addSteps, BigDecimal addEnergyJ, BigDecimal addStoredJ,
                           BigDecimal addExportedJ, BigDecimal powerW) {
        this.steps += addSteps;
        this.energyJ = energyJ.add(addEnergyJ).setScale(3, RoundingMode.HALF_UP);
        this.storedJ = storedJ.add(addStoredJ).setScale(3, RoundingMode.HALF_UP);
        this.exportedJ = exportedJ.add(addExportedJ).setScale(3, RoundingMode.HALF_UP);
        this.peakPowerW = peakPowerW.max(powerW.setScale(3, RoundingMode.HALF_UP));
        this.telemetryCount += 1;
    }
}
