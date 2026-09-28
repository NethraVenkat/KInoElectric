package com.backend.javaleap.service;

import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.InvoiceLine;
import com.backend.javaleap.entity.Payment;
import com.backend.javaleap.entity.PurchaseOrder;
import com.backend.javaleap.entity.PurchaseOrderLine;
import com.backend.javaleap.entity.SalesOrder;
import com.backend.javaleap.entity.SalesOrderLine;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.model.response.InvoiceLineResponse;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.OrderLineResponse;
import com.backend.javaleap.model.response.PaymentResponse;
import com.backend.javaleap.model.response.PurchaseOrderResponse;
import com.backend.javaleap.model.response.SalesOrderResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Orders, invoices and payments -> DTOs. */
@Component
public class TransactionMapper {

    public PurchaseOrderResponse toResponse(PurchaseOrder p, Invoice bill, boolean withLines) {
        return new PurchaseOrderResponse(p.getId(), p.getPoNumber(), p.getStatus(), p.getVendor().getId(),
                p.getVendor().getCode(), p.getVendor().getName(), p.getOrderDate(), p.getExpectedDate(),
                p.getAnalyticAccount() == null ? null : p.getAnalyticAccount().getCode(),
                p.getReference(), p.getNotes(), p.getUntaxedAmount(), p.getTaxAmount(), p.getTotalAmount(),
                bill == null ? null : bill.getId(), bill == null ? null : bill.getInvoiceNumber(),
                p.getConfirmedAt(), username(p.getCreatedBy()),
                withLines ? p.getLines().stream().map(this::toLine).toList() : List.of());
    }

    public SalesOrderResponse toResponse(SalesOrder s, Invoice invoice, boolean withLines) {
        return new SalesOrderResponse(s.getId(), s.getSoNumber(), s.getStatus(), s.getCustomer().getId(),
                s.getCustomer().getCode(), s.getCustomer().getName(), s.getOrderDate(),
                s.getStation() == null ? null : s.getStation().getCode(), s.getPeriodStart(), s.getPeriodEnd(),
                s.getReference(), s.getNotes(), s.getUntaxedAmount(), s.getTaxAmount(), s.getTotalAmount(),
                invoice == null ? null : invoice.getId(), invoice == null ? null : invoice.getInvoiceNumber(),
                s.getConfirmedAt(), username(s.getCreatedBy()),
                withLines ? s.getLines().stream().map(this::toLine).toList() : List.of());
    }

    public InvoiceResponse toResponse(Invoice i, List<Payment> payments, boolean withLines) {
        String source = i.getSalesOrder() != null ? i.getSalesOrder().getSoNumber()
                : i.getPurchaseOrder() != null ? i.getPurchaseOrder().getPoNumber() : null;
        return new InvoiceResponse(i.getId(), i.getInvoiceType(), i.getInvoiceNumber(), i.getStatus(),
                i.getPaymentState(), i.getContact().getId(), i.getContact().getCode(), i.getContact().getName(),
                i.getContact().getGstin(), i.getInvoiceDate(), i.getDueDate(), i.overdueDays(LocalDate.now()),
                source, i.getVendorReference(), i.getPlaceOfSupply(), i.getUntaxedAmount(), i.getCgstAmount(),
                i.getSgstAmount(), i.getIgstAmount(), i.getTaxAmount(), i.getTotalAmount(), i.getAmountPaid(),
                i.getAmountDue(), i.getJournalEntry() == null ? null : i.getJournalEntry().getId(), i.getNotes(),
                i.getPostedAt(),
                withLines ? i.getLines().stream().map(this::toLine).toList() : List.of(),
                payments.stream().map(this::toResponse).toList());
    }

    public PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getPaymentNumber(), p.getPaymentType(), p.getStatus(),
                p.getContact().getId(), p.getContact().getName(), p.getInvoice().getId(),
                p.getInvoice().getInvoiceNumber(), p.getPaymentDate(), p.getAmount(), p.getMethod(),
                p.getJournal().getCode(), p.getReference(),
                p.getJournalEntry() == null ? null : p.getJournalEntry().getId(), p.getNotes(), p.getCreatedAt());
    }

    private OrderLineResponse toLine(PurchaseOrderLine l) {
        return new OrderLineResponse(l.getLineNo(), l.getProduct().getCode(), l.getProduct().getName(),
                l.getDescription(), l.getTile() == null ? null : l.getTile().getTileCode(),
                l.getAnalyticAccount() == null ? null : l.getAnalyticAccount().getCode(),
                l.getQuantity(), l.getProduct().getUom(), l.getUnitPrice(), l.getGstRate(),
                l.getSubtotal(), l.getTaxAmount(), l.getTotal());
    }

    private OrderLineResponse toLine(SalesOrderLine l) {
        return new OrderLineResponse(l.getLineNo(), l.getProduct().getCode(), l.getProduct().getName(),
                l.getDescription(), null,
                l.getAnalyticAccount() == null ? null : l.getAnalyticAccount().getCode(),
                l.getQuantity(), l.getProduct().getUom(), l.getUnitPrice(), l.getGstRate(),
                l.getSubtotal(), l.getTaxAmount(), l.getTotal());
    }

    private InvoiceLineResponse toLine(InvoiceLine l) {
        return new InvoiceLineResponse(l.getLineNo(), l.getProduct() == null ? null : l.getProduct().getCode(),
                l.getDescription(), l.getAccount().getCode(), l.getAccount().getName(),
                l.getAnalyticAccount() == null ? null : l.getAnalyticAccount().getCode(),
                l.getQuantity(), l.getUnitPrice(), l.getGstRate(), l.getSubtotal(), l.getTaxAmount(), l.getTotal());
    }

    private static String username(User u) {
        return u == null ? null : u.getUsername();
    }
}
