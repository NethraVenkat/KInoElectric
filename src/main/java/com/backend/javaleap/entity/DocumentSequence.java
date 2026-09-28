package com.backend.javaleap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Gap-free yearly numbering for documents that are not journal entries (PO, SO). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "document_sequence")
public class DocumentSequence {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 10)
    private String prefix;

    @Column(name = "seq_year", nullable = false)
    private Integer seqYear;

    @Column(name = "next_number", nullable = false)
    private Integer nextNumber;

    /** Consumes the next number, e.g. PO/2026/0003. Caller must hold a row lock. */
    public String take(LocalDate date) {
        if (seqYear == null || date.getYear() > seqYear) {
            seqYear = date.getYear();
            nextNumber = 1;
        }
        String number = String.format("%s/%d/%04d", prefix, date.getYear(), nextNumber);
        nextNumber = nextNumber + 1;
        return number;
    }
}
