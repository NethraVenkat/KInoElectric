package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.BatteryStatus;

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

/** Energy storage bank charged by the tiles. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "battery")
public class Battery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String chemistry = "LiFePO4";

    @Column(name = "capacity_wh", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityWh;

    @Column(name = "charge_wh", nullable = false, precision = 10, scale = 3)
    private BigDecimal chargeWh;

    @Column(name = "max_charge_rate_w", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxChargeRateW;

    @Builder.Default
    @Column(name = "temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperatureC = BigDecimal.valueOf(28);

    @Builder.Default
    @Column(name = "cycle_count", nullable = false)
    private Integer cycleCount = 0;

    @Builder.Default
    @Column(name = "health_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal healthPct = BigDecimal.valueOf(100);

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BatteryStatus status = BatteryStatus.IDLE;

    @Version
    @Column(nullable = false)
    private Long version;

    // ------------------------------------------------------------------ domain logic

    /** State of charge in percent (0-100). */
    public BigDecimal getChargePct() {
        if (capacityWh == null || capacityWh.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return chargeWh.multiply(BigDecimal.valueOf(100)).divide(capacityWh, 2, RoundingMode.HALF_UP);
    }

    /**
     * Adds (positive) or removes (negative) energy, clamped between 0 and capacity.
     *
     * @return the energy actually applied in Wh
     */
    public BigDecimal applyEnergyWh(BigDecimal deltaWh) {
        BigDecimal before = chargeWh;
        BigDecimal after = chargeWh.add(deltaWh).max(BigDecimal.ZERO).min(capacityWh);
        this.chargeWh = after.setScale(3, RoundingMode.HALF_UP);
        return after.subtract(before);
    }
}
