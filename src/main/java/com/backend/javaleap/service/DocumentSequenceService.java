package com.backend.javaleap.service;

import com.backend.javaleap.entity.DocumentSequence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Gap-free PO / SO numbers under a row lock. */
@Service
@RequiredArgsConstructor
public class DocumentSequenceService {

    public static final String PURCHASE_ORDER = "PO";
    public static final String SALES_ORDER = "SO";

    private final EntityManager entityManager;

    @Transactional
    public String next(String code, LocalDate date) {
        DocumentSequence seq = entityManager.find(DocumentSequence.class, code, LockModeType.PESSIMISTIC_WRITE);
        if (seq == null) {
            // Self-healing: create the counter on first use (e.g. fresh database without seed rows)
            seq = DocumentSequence.builder().code(code).prefix(code).seqYear(date.getYear()).nextNumber(1).build();
            entityManager.persist(seq);
            entityManager.flush();
        }
        return seq.take(date);
    }
}
