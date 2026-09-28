package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.InvoiceType;
import com.backend.javaleap.entity.enums.PaymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = {"contact", "contact.receivableAccount", "contact.payableAccount", "salesOrder",
            "purchaseOrder", "journalEntry", "lines", "lines.product", "lines.account", "lines.analyticAccount"})
    Optional<Invoice> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"contact", "salesOrder", "purchaseOrder"})
    @Query("""
            select i from Invoice i
            where (:type is null or i.invoiceType = :type)
              and (:status is null or i.status = :status)
              and (:paymentState is null or i.paymentState = :paymentState)
              and (:contactId is null or i.contact.id = :contactId)
              and (:overdueBefore is null or (i.dueDate < :overdueBefore
                   and i.status = com.backend.javaleap.entity.enums.InvoiceStatus.POSTED
                   and i.paymentState <> com.backend.javaleap.entity.enums.PaymentState.PAID))
              and (:q is null or lower(i.invoiceNumber) like lower(concat('%', :q, '%'))
                   or lower(i.vendorReference) like lower(concat('%', :q, '%'))
                   or lower(i.contact.name) like lower(concat('%', :q, '%')))
            """)
    Page<Invoice> search(@Param("type") InvoiceType type, @Param("status") InvoiceStatus status,
                         @Param("paymentState") PaymentState paymentState, @Param("contactId") Long contactId,
                         @Param("overdueBefore") LocalDate overdueBefore, @Param("q") String q, Pageable pageable);

    /** Latest non-cancelled invoice created from an order. */
    Optional<Invoice> findFirstByPurchaseOrderIdAndStatusNotOrderByIdDesc(Long purchaseOrderId, InvoiceStatus status);

    Optional<Invoice> findFirstBySalesOrderIdAndStatusNotOrderByIdDesc(Long salesOrderId, InvoiceStatus status);

    /** Open amount owed by / to contacts of a type (receivables or payables). */
    @Query("""
            select coalesce(sum(i.amountDue), 0) from Invoice i
            where i.invoiceType = :type and i.status = com.backend.javaleap.entity.enums.InvoiceStatus.POSTED
            """)
    BigDecimal sumOpenAmount(@Param("type") InvoiceType type);
}
