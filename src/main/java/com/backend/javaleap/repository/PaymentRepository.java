package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Payment;
import com.backend.javaleap.entity.enums.PaymentStatus;
import com.backend.javaleap.entity.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"contact", "contact.receivableAccount", "contact.payableAccount", "invoice",
            "journal", "journal.defaultAccount", "journalEntry"})
    Optional<Payment> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"contact", "invoice", "journal", "journalEntry"})
    List<Payment> findByInvoiceIdOrderByPaymentDateAscIdAsc(Long invoiceId);

    boolean existsByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);

    @EntityGraph(attributePaths = {"contact", "invoice", "journal"})
    @Query("""
            select p from Payment p
            where (:type is null or p.paymentType = :type)
              and (:status is null or p.status = :status)
              and (:contactId is null or p.contact.id = :contactId)
              and (:from is null or p.paymentDate >= :from)
              and (:to is null or p.paymentDate <= :to)
            """)
    Page<Payment> search(@Param("type") PaymentType type, @Param("status") PaymentStatus status,
                         @Param("contactId") Long contactId, @Param("from") LocalDate from,
                         @Param("to") LocalDate to, Pageable pageable);
}
