package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.InvoiceLine;
import com.backend.javaleap.entity.JournalEntry;
import com.backend.javaleap.entity.PurchaseOrder;
import com.backend.javaleap.entity.PurchaseOrderLine;
import com.backend.javaleap.entity.SalesOrder;
import com.backend.javaleap.entity.SalesOrderLine;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.InvoiceType;
import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.entity.enums.PaymentState;
import com.backend.javaleap.entity.enums.PaymentStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.InvoiceUpdateRequest;
import com.backend.javaleap.model.request.ReverseEntryRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.repository.InvoiceRepository;
import com.backend.javaleap.repository.PaymentRepository;
import com.backend.javaleap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer invoices and vendor bills.
 * <pre>
 * Customer invoice (SAL):  Dr Receivable (grid)      total
 *                          Cr Income (per line)      subtotal   [analytic = floor sector]
 *                          Cr GST Output             tax
 * Vendor bill (PUR):       Dr Expense/Asset (line)   subtotal   [analytic = station zone]
 *                          Dr GST Input              tax
 *                          Cr Payable (vendor)       total
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;
    private final AccountService accountService;
    private final TaxCalculator taxCalculator;
    private final TransactionMapper mapper;

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(InvoiceType type, InvoiceStatus status, PaymentState paymentState,
                                                Long contactId, boolean overdueOnly, String q, Pageable pageable) {
        return PageResponse.from(invoiceRepository.search(type, status, paymentState, contactId,
                        overdueOnly ? LocalDate.now() : null, q == null || q.isBlank() ? null : q.trim(), pageable),
                i -> mapper.toResponse(i, List.of(), false));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id) {
        Invoice i = find(id);
        return mapper.toResponse(i, paymentRepository.findByInvoiceIdOrderByPaymentDateAscIdAsc(id), true);
    }

    // ------------------------------------------------------------------ creation from orders

    @Transactional
    public Invoice createFromPurchaseOrder(PurchaseOrder po, CreateInvoiceRequest req) {
        List<InvoiceLine> lines = new ArrayList<>();
        for (PurchaseOrderLine l : po.getLines()) {
            Account account = l.getProduct().getExpenseAccount();
            if (account == null) {
                throw new BadRequestException("Product " + l.getProduct().getCode() + " has no expense account");
            }
            lines.add(InvoiceLine.builder().product(l.getProduct()).description(l.getDescription())
                    .account(account).analyticAccount(l.getAnalyticAccount())
                    .quantity(l.getQuantity()).unitPrice(l.getUnitPrice()).gstRate(l.getGstRate())
                    .subtotal(l.getSubtotal()).taxAmount(l.getTaxAmount()).total(l.getTotal()).build());
        }
        Invoice bill = newInvoice(InvoiceType.VENDOR_BILL, po.getVendor(), req);
        bill.setPurchaseOrder(po);
        bill.setVendorReference(req == null ? null : trim(req.vendorReference()));
        return save(bill, lines);
    }

    @Transactional
    public Invoice createFromSalesOrder(SalesOrder so, CreateInvoiceRequest req) {
        List<InvoiceLine> lines = new ArrayList<>();
        for (SalesOrderLine l : so.getLines()) {
            Account account = l.getProduct().getIncomeAccount();
            if (account == null) {
                throw new BadRequestException("Product " + l.getProduct().getCode() + " has no income account");
            }
            lines.add(InvoiceLine.builder().product(l.getProduct()).description(l.getDescription())
                    .account(account).analyticAccount(l.getAnalyticAccount())
                    .quantity(l.getQuantity()).unitPrice(l.getUnitPrice()).gstRate(l.getGstRate())
                    .subtotal(l.getSubtotal()).taxAmount(l.getTaxAmount()).total(l.getTotal()).build());
        }
        Invoice invoice = newInvoice(InvoiceType.CUSTOMER_INVOICE, so.getCustomer(), req);
        invoice.setSalesOrder(so);
        if (so.isEnergyExport()) {
            invoice.setNotes("Kinetic energy exported from " + so.getStation().getName() + ", "
                    + so.getPeriodStart() + " to " + so.getPeriodEnd());
        }
        return save(invoice, lines);
    }

    private Invoice newInvoice(InvoiceType type, Contact contact, CreateInvoiceRequest req) {
        LocalDate date = req == null || req.invoiceDate() == null ? LocalDate.now() : req.invoiceDate();
        return Invoice.builder()
                .invoiceType(type)
                .contact(contact)
                .invoiceDate(date)
                .dueDate(date.plusDays(contact.getPaymentTermsDays() == null ? 30 : contact.getPaymentTermsDays()))
                .status(InvoiceStatus.DRAFT)
                .createdBy(currentUser())
                .build();
    }

    private Invoice save(Invoice invoice, List<InvoiceLine> lines) {
        invoice.replaceLines(lines);
        TaxCalculator.GstSplit gst = taxCalculator.split(invoice.getTaxAmount(), invoice.getContact());
        invoice.setPlaceOfSupply(gst.placeOfSupply());
        invoice.setCgstAmount(gst.cgst());
        invoice.setSgstAmount(gst.sgst());
        invoice.setIgstAmount(gst.igst());
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Draft {} {} created for {} ({} INR)", saved.getInvoiceType(), saved.getId(),
                saved.getContact().getName(), saved.getTotalAmount());
        return saved;
    }

    // ------------------------------------------------------------------ draft edits

    @Transactional
    public InvoiceResponse updateDraft(Long id, InvoiceUpdateRequest req) {
        Invoice i = find(id);
        requireStatus(i, InvoiceStatus.DRAFT);
        if (req.invoiceDate() != null) {
            i.setInvoiceDate(req.invoiceDate());
            if (req.dueDate() == null) {
                i.setDueDate(req.invoiceDate().plusDays(i.getContact().getPaymentTermsDays()));
            }
        }
        if (req.dueDate() != null) {
            i.setDueDate(req.dueDate());
        }
        if (i.getDueDate().isBefore(i.getInvoiceDate())) {
            throw new BadRequestException("dueDate cannot be before invoiceDate");
        }
        if (req.vendorReference() != null) {
            i.setVendorReference(trim(req.vendorReference()));
        }
        if (req.notes() != null) {
            i.setNotes(trim(req.notes()));
        }
        return mapper.toResponse(i, List.of(), true);
    }

    // ------------------------------------------------------------------ posting

    @Transactional
    public InvoiceResponse post(Long id) {
        Invoice i = find(id);
        requireStatus(i, InvoiceStatus.DRAFT);
        if (i.getTotalAmount().signum() <= 0) {
            throw new BadRequestException("Cannot post an invoice with a zero total");
        }
        Contact contact = i.getContact();
        String label = i.isCustomerInvoice() ? "Invoice " : "Bill ";
        List<PostingLine> lines = new ArrayList<>();

        if (i.isCustomerInvoice()) {
            Account receivable = contact.getReceivableAccount() != null ? contact.getReceivableAccount()
                    : accountService.defaultFor(AccountSubType.RECEIVABLE);
            lines.add(PostingLine.debit(receivable, i.getTotalAmount(), contact.getName()).withContact(contact));
            for (InvoiceLine l : i.getLines()) {
                lines.add(PostingLine.credit(l.getAccount(), l.getSubtotal(), l.getDescription())
                        .withAnalytic(l.getAnalyticAccount()));
            }
            if (i.getTaxAmount().signum() > 0) {
                lines.add(PostingLine.credit(accountService.defaultFor(AccountSubType.TAX_OUTPUT), i.getTaxAmount(),
                        "GST output"));
            }
        } else {
            Account payable = contact.getPayableAccount() != null ? contact.getPayableAccount()
                    : accountService.defaultFor(AccountSubType.PAYABLE);
            for (InvoiceLine l : i.getLines()) {
                lines.add(PostingLine.debit(l.getAccount(), l.getSubtotal(), l.getDescription())
                        .withAnalytic(l.getAnalyticAccount()));
            }
            if (i.getTaxAmount().signum() > 0) {
                lines.add(PostingLine.debit(accountService.defaultFor(AccountSubType.TAX_INPUT), i.getTaxAmount(),
                        "GST input credit"));
            }
            lines.add(PostingLine.credit(payable, i.getTotalAmount(), contact.getName()).withContact(contact));
        }
        lines.removeIf(l -> l.debit().signum() == 0 && l.credit().signum() == 0);

        JournalEntry entry = journalEntryService.createPosted(
                i.isCustomerInvoice() ? "SAL" : "PUR",
                i.getInvoiceDate(),
                i.isCustomerInvoice() ? sourceNumber(i) : firstNonNull(i.getVendorReference(), sourceNumber(i)),
                label + "to/from " + contact.getName(),
                i.isCustomerInvoice() ? EntrySourceType.CUSTOMER_INVOICE : EntrySourceType.VENDOR_BILL,
                i.getId(), lines);

        i.setJournalEntry(entry);
        i.setInvoiceNumber(entry.getEntryNumber());
        i.setStatus(InvoiceStatus.POSTED);
        i.setAmountDue(i.getTotalAmount().subtract(i.getAmountPaid()));
        i.setPostedAt(LocalDateTime.now());
        i.setPostedBy(currentUser());
        log.info("{} {} posted ({} INR)", i.getInvoiceType(), i.getInvoiceNumber(), i.getTotalAmount());
        return mapper.toResponse(i, List.of(), true);
    }

    /**
     * Cancels an invoice. A posted invoice without payments is reversed in the books;
     * the source order goes back to CONFIRMED so it can be invoiced again.
     */
    @Transactional
    public InvoiceResponse cancel(Long id) {
        Invoice i = find(id);
        if (i.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Invoice is already cancelled");
        }
        if (i.getStatus() == InvoiceStatus.POSTED) {
            if (paymentRepository.existsByInvoiceIdAndStatus(id, PaymentStatus.POSTED)) {
                throw new BadRequestException("Cancel the payments of " + i.getInvoiceNumber() + " first");
            }
            LocalDate date = LocalDate.now().isBefore(i.getInvoiceDate()) ? i.getInvoiceDate() : LocalDate.now();
            journalEntryService.reverse(i.getJournalEntry().getId(),
                    new ReverseEntryRequest(date, "Cancellation of " + i.getInvoiceNumber()));
        }
        i.setStatus(InvoiceStatus.CANCELLED);
        i.setAmountDue(BigDecimal.ZERO);
        if (i.getPurchaseOrder() != null && i.getPurchaseOrder().getStatus() == OrderStatus.INVOICED) {
            i.getPurchaseOrder().setStatus(OrderStatus.CONFIRMED);
        }
        if (i.getSalesOrder() != null && i.getSalesOrder().getStatus() == OrderStatus.INVOICED) {
            i.getSalesOrder().setStatus(OrderStatus.CONFIRMED);
        }
        log.info("Invoice {} cancelled", i.getInvoiceNumber() == null ? "#" + id : i.getInvoiceNumber());
        return mapper.toResponse(i, List.of(), true);
    }

    // ------------------------------------------------------------------ helpers

    Invoice find(Long id) {
        return invoiceRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
    }

    private static void requireStatus(Invoice i, InvoiceStatus expected) {
        if (i.getStatus() != expected) {
            throw new BadRequestException("Invoice is " + i.getStatus() + " - expected " + expected);
        }
    }

    private static String sourceNumber(Invoice i) {
        return i.getSalesOrder() != null ? i.getSalesOrder().getSoNumber()
                : i.getPurchaseOrder() != null ? i.getPurchaseOrder().getPoNumber() : null;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private User currentUser() {
        return SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
    }
}
