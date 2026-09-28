package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Product master: harvested kWh, kinetic floor tiles, transducer overhaul services. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "product_type", nullable = false, length = 10)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private UnitOfMeasure uom;

    @Column(name = "hsn_sac", length = 10)
    private String hsnSac;

    @Builder.Default
    @Column(name = "sale_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "purchase_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "can_be_sold", nullable = false)
    private Boolean canBeSold = Boolean.FALSE;

    @Builder.Default
    @Column(name = "can_be_purchased", nullable = false)
    private Boolean canBePurchased = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_account_id")
    private Account incomeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_id")
    private Account expenseAccount;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;
}
