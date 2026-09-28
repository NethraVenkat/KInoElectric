package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.ZoneType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * An area inside a station (entry gates, stairs, platform...).
 * pos/width/height describe its rectangle on the floor-map canvas,
 * gridRows x gridCols describe the tile grid inside it.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "zone")
public class Zone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "zone_type", nullable = false, length = 20)
    private ZoneType zoneType;

    /** Relative share of station footfall - drives the simulator. */
    @Builder.Default
    @Column(name = "traffic_weight", nullable = false, precision = 4, scale = 2)
    private BigDecimal trafficWeight = BigDecimal.ONE;

    /** Safe number of people in this zone (used by crowd safety). */
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "pos_x", nullable = false)
    private Integer posX;

    @Column(name = "pos_y", nullable = false)
    private Integer posY;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "grid_rows", nullable = false)
    private Integer gridRows;

    @Column(name = "grid_cols", nullable = false)
    private Integer gridCols;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "zone")
    @OrderBy("gridRow ASC, gridCol ASC")
    private List<Tile> tiles = new ArrayList<>();
}
