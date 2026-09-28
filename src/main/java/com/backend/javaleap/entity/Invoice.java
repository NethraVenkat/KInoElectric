package com.backend.javaleap.entity;

import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.InvoiceType;
import com.backend.javaleap.entity.enums.PaymentState;
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
 * Customer invoice or vendor bill. Posting creates its journal entry and takes the entry number
 * (INV/2026/0001, BILL/2026/0001); payments reduce {@code amountDue}.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType invoiceType;

    @Column(name = "invoice_number", unique = true, length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "payment_state", nullable = false, length = 20)
    private PaymentState paymentState = PaymentState.NOT_PAID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Column(name = "vendor_reference", length = 60)
    private String vendorReference;

    /** GST state code of the contact (e.g. 33 = Tamil Nadu). */
    @Column(name = "place_of_supply", length = 2)
    private String placeOfSupply;

    @Builder.Default
    @Column(name = "untaxed_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal untaxedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cgst_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sgst_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "igst_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_paid", nullable = false, precision = 16, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_due", nullable = false, precision = 16, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    // ------------------------------------------------------------------ domain logic

    public boolean isCustomerInvoice() {
        return invoiceType == InvoiceType.CUSTOMER_INVOICE;
    }

    /** Replaces the lines and recomputes untaxed / tax / total (GST split is set by the caller). */
    public void replaceLines(List<InvoiceLine> newLines) {
        lines.clear();
        int n = 1;
        for (InvoiceLine l : newLines) {
            l.setInvoice(this);
            l.setLineNo(n++);
            lines.add(l);
        }
        untaxedAmount = lines.stream().map(InvoiceLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        taxAmount = lines.stream().map(InvoiceLine::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAmount = untaxedAmount.add(taxAmount);
        amountDue = totalAmount.subtract(amountPaid);
    }

    /** Registers a payment and updates the payment state. */
    public void applyPayment(BigDecimal amount) {
        amountPaid = amountPaid.add(amount);
        refreshPaymentState();
    }

    /** Undoes a (cancelled) payment. */
    public void removePayment(BigDecimal amount) {
        amountPaid = amountPaid.subtract(amount).max(BigDecimal.ZERO);
        refreshPaymentState();
    }

    private void refreshPaymentState() {
        amountDue = totalAmount.subtract(amountPaid);
        if (amountPaid.signum() == 0) {
            paymentState = PaymentState.NOT_PAID;
        } else if (amountDue.signum() <= 0) {
            paymentState = PaymentState.PAID;
        } else {
            paymentState = PaymentState.PARTIAL;
        }
    }

    /** Days past due for an unpaid posted invoice (0 if not overdue). */
    public long overdueDays(LocalDate today) {
        if (status != InvoiceStatus.POSTED || paymentState == PaymentState.PAID || !today.isAfter(dueDate)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
    }
}
