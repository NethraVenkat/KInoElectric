package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.OrderStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Purchase order to a tile vendor: DRAFT -> CONFIRMED (numbered) -> INVOICED (vendor bill created). */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Contact vendor;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    /** Default station zone (analytic account) for lines that do not set their own. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analytic_account_id")
    private AnalyticAccount analyticAccount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "untaxed_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal untaxedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    /** Replaces all lines and recomputes the totals. */
    public void replaceLines(List<PurchaseOrderLine> newLines) {
        lines.clear();
        int n = 1;
        for (PurchaseOrderLine l : newLines) {
            l.setPurchaseOrder(this);
            l.setLineNo(n++);
            lines.add(l);
        }
        untaxedAmount = lines.stream().map(PurchaseOrderLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        taxAmount = lines.stream().map(PurchaseOrderLine::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAmount = untaxedAmount.add(taxAmount);
    }
}
