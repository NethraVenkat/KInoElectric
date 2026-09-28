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

/** Live station-level snapshot written every simulation tick (kept for 24 h). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reading")
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private Integer steps;

    @Column(name = "energy_j", nullable = false, precision = 12, scale = 3)
    private BigDecimal energyJ;

    @Column(name = "power_w", nullable = false, precision = 10, scale = 3)
    private BigDecimal powerW;

    @Column(name = "battery_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal batteryPct;

    @Column(name = "active_tiles", nullable = false)
    private Integer activeTiles;
}
