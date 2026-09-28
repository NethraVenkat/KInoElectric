package com.backend.javaleap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Hourly roll-up of station energy - long-term history for analytics and reports. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "energy_summary_hourly")
public class EnergySummaryHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(nullable = false)
    private Long steps;

    @Column(name = "energy_j", nullable = false, precision = 16, scale = 3)
    private BigDecimal energyJ;

    @Column(name = "avg_power_w", nullable = false, precision = 10, scale = 3)
    private BigDecimal avgPowerW;

    @Column(name = "peak_power_w", nullable = false, precision = 10, scale = 3)
    private BigDecimal peakPowerW;

    /** Estimated passengers in the hour. */
    @Column(nullable = false)
    private Integer footfall;
}
