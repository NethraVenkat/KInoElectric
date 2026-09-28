package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.JournalType;
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

import java.time.LocalDate;

/** Journal (Sales, Purchase, Cash, Bank, Misc) with its own gap-free numbering per year. */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "journal")
public class Journal extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "journal_type", nullable = false, length = 20)
    private JournalType journalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_account_id")
    private Account defaultAccount;

    @Column(name = "sequence_prefix", nullable = false, unique = true, length = 10)
    private String sequencePrefix;

    @Column(name = "sequence_year", nullable = false)
    private Integer sequenceYear;

    @Builder.Default
    @Column(name = "next_number", nullable = false)
    private Integer nextNumber = 1;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    /**
     * Consumes the next number, e.g. {@code INV/2026/0007}. Numbering restarts at 1 each calendar year.
     * Callers must hold a pessimistic lock on this journal row.
     */
    public String takeNextNumber(LocalDate entryDate) {
        int year = entryDate.getYear();
        if (sequenceYear == null || year > sequenceYear) {
            sequenceYear = year;
            nextNumber = 1;
        }
        String number = String.format("%s/%d/%04d", sequencePrefix, year, nextNumber);
        nextNumber = nextNumber + 1;
        return number;
    }

    /** What the next number will look like (for display only). */
    public String previewNextNumber() {
        return String.format("%s/%d/%04d", sequencePrefix, sequenceYear, nextNumber);
    }
}
