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

/** Live per-zone snapshot - crowd density and energy by zone. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "zone_reading")
public class ZoneReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private Integer steps;

    @Column(name = "energy_j", nullable = false, precision = 12, scale = 3)
    private BigDecimal energyJ;

    /** Estimated number of people in the zone. */
    @Column(nullable = false)
    private Integer occupancy;

    /** occupancy / max_capacity * 100 */
    @Column(name = "density_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal densityPct;
}
