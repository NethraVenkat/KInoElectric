package com.backend.javaleap.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kinetic conversion rate per tile technology, set by the Transit Energy Director.
 * <pre>energy per step (J) = joulesPerStep x conversionEfficiency x tileHealth%</pre>
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "kinetic_conversion_rate")
public class KineticConversionRate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tile_type", nullable = false, unique = true, length = 20)
    private TileType tileType;

    @Column(name = "joules_per_step", nullable = false, precision = 5, scale = 2)
    private BigDecimal joulesPerStep;

    @Column(name = "conversion_efficiency", nullable = false, precision = 4, scale = 3)
    private BigDecimal conversionEfficiency;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    /** Electrical joules delivered per step by a 100%-healthy tile. */
    public BigDecimal getEffectiveJoulesPerStep() {
        return joulesPerStep.multiply(conversionEfficiency).setScale(3, RoundingMode.HALF_UP);
    }
}
