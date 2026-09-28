package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.EntryStatus;
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

/**
 * Double-entry voucher. A POSTED entry always has total debit = total credit and is immutable;
 * mistakes are corrected with a reversal entry.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "journal_entry")
public class JournalEntry extends BaseEntity {

    /** Assigned on posting, e.g. INV/2026/0001. */
    @Column(name = "entry_number", unique = true, length = 30)
    private String entryNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String narration;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EntryStatus status = EntryStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source_type", nullable = false, length = 30)
    private EntrySourceType sourceType = EntrySourceType.MANUAL;

    @Column(name = "source_id")
    private Long sourceId;

    @Builder.Default
    @Column(name = "total_debit", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_credit", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id")
    private JournalEntry reversalOf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<JournalItem> items = new ArrayList<>();

    // ------------------------------------------------------------------ domain logic

    /** Replaces all lines and recomputes totals. */
    public void replaceItems(List<JournalItem> newItems) {
        items.clear();
        int line = 1;
        for (JournalItem item : newItems) {
            item.setEntry(this);
            item.setLineNo(line++);
            items.add(item);
        }
        recalculateTotals();
    }

    public void recalculateTotals() {
        totalDebit = items.stream().map(JournalItem::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCredit = items.stream().map(JournalItem::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isBalanced() {
        return totalDebit.signum() > 0 && totalDebit.compareTo(totalCredit) == 0;
    }

    public boolean isDraft() {
        return status == EntryStatus.DRAFT;
    }
}
