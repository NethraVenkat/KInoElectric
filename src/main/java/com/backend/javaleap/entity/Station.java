package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.StationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** A metro station fitted with the kinetic floor network. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "station")
public class Station extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(name = "line_name", length = 80)
    private String lineName;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "daily_footfall_estimate", nullable = false)
    private Integer dailyFootfallEstimate = 0;

    /** Floor-map canvas width in SVG units. */
    @Builder.Default
    @Column(name = "layout_width", nullable = false)
    private Integer layoutWidth = 1000;

    /** Floor-map canvas height in SVG units. */
    @Builder.Default
    @Column(name = "layout_height", nullable = false)
    private Integer layoutHeight = 740;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StationStatus status = StationStatus.ONLINE;

    @Builder.Default
    @OneToMany(mappedBy = "station")
    @OrderBy("displayOrder ASC")
    private List<Zone> zones = new ArrayList<>();
}
