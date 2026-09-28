package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Invoice;
import com.backend.javaleap.entity.Journal;
import com.backend.javaleap.entity.JournalEntry;
import com.backend.javaleap.entity.Payment;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.JournalType;
import com.backend.javaleap.entity.enums.PaymentMethod;
import com.backend.javaleap.entity.enums.PaymentState;
import com.backend.javaleap.entity.enums.PaymentStatus;
import com.backend.javaleap.entity.enums.PaymentType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.PaymentRequest;
import com.backend.javaleap.model.request.ReverseEntryRequest;
import com.backend.javaleap.model.response.PaymentResponse;
import com.backend.javaleap.repository.PaymentRepository;
import com.backend.javaleap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payments against posted invoices, through the Bank (or Cash) journal.
 * <pre>
 * Customer receipt (INBOUND):  Dr Bank              Cr Receivable (grid)
 * Vendor payment (OUTBOUND):   Dr Payable (vendor)  Cr Bank
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InvoiceService invoiceService;
    private final JournalService journalService;
    private final JournalEntryService journalEntryService;
    private final AccountService accountService;
    private final TransactionMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> search(PaymentType type, PaymentStatus status, Long contactId,
                                                LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(paymentRepository.search(type, status, contactId, from, to, pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PaymentResponse register(PaymentRequest req) {
        Invoice invoice = invoiceService.find(req.invoiceId());
        if (invoice.getStatus() != InvoiceStatus.POSTED) {
            throw new BadRequestException("Only posted invoices can be paid");
        }
        if (invoice.getPaymentState() == PaymentState.PAID) {
            throw new BadRequestException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid");
        }
        BigDecimal amount = req.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(invoice.getAmountDue()) > 0) {
            throw new BadRequestException("Amount " + amount + " exceeds the amount due " + invoice.getAmountDue());
        }
        LocalDate date = req.paymentDate() == null ? LocalDate.now() : req.paymentDate();
        if (date.isBefore(invoice.getInvoiceDate())) {
            throw new BadRequestException("Payment date cannot be before the invoice date");
        }

        String journalCode = req.journalCode() != null && !req.journalCode().isBlank() ? req.journalCode()
                : req.method() == PaymentMethod.CASH ? "CSH" : "BNK";
        Journal journal = journalService.require(journalCode);
        if (journal.getJournalType() != JournalType.BANK && journal.getJournalType() != JournalType.CASH) {
            throw new BadRequestException("Payments must use a BANK or CASH journal");
        }
        Account cashOrBank = journal.getDefaultAccount();
        if (cashOrBank == null) {
            throw new BadRequestException("Journal " + journal.getCode() + " has no default bank/cash account");
        }

        Contact contact = invoice.getContact();
        boolean inbound = invoice.isCustomerInvoice();
        String label = (inbound ? "Receipt from " : "Payment to ") + contact.getName() + " for " + invoice.getInvoiceNumber();
        List<PostingLine> lines;
        if (inbound) {
            Account receivable = contact.getReceivableAccount() != null ? contact.getReceivableAccount()
                    : accountService.defaultFor(AccountSubType.RECEIVABLE);
            lines = List.of(PostingLine.debit(cashOrBank, amount, label),
                    PostingLine.credit(receivable, amount, label).withContact(contact));
        } else {
            Account payable = contact.getPayableAccount() != null ? contact.getPayableAccount()
                    : accountService.defaultFor(AccountSubType.PAYABLE);
            lines = List.of(PostingLine.debit(payable, amount, label).withContact(contact),
                    PostingLine.credit(cashOrBank, amount, label));
        }

        JournalEntry entry = journalEntryService.createPosted(journal.getCode(), date,
                req.reference() == null || req.reference().isBlank() ? invoice.getInvoiceNumber() : req.reference().trim(),
                label, EntrySourceType.PAYMENT, null, lines);

        Payment payment = paymentRepository.save(Payment.builder()
                .paymentNumber(entry.getEntryNumber())
                .paymentType(inbound ? PaymentType.INBOUND : PaymentType.OUTBOUND)
                .contact(contact)
                .invoice(invoice)
                .paymentDate(date)
                .amount(amount)
                .method(req.method())
                .journal(journal)
                .reference(req.reference() == null ? null : req.reference().trim())
                .status(PaymentStatus.POSTED)
                .journalEntry(entry)
                .notes(req.notes())
                .createdBy(currentUser())
                .build());
        entry.setSourceId(payment.getId());
        invoice.applyPayment(amount);

        log.info("{} {} of {} INR on {} ({})", payment.getPaymentType(), payment.getPaymentNumber(), amount,
                invoice.getInvoiceNumber(), invoice.getPaymentState());
        return mapper.toResponse(payment);
    }

    /** Cancels a payment: reverses its journal entry and re-opens the invoice amount. */
    @Transactional
    public PaymentResponse cancel(Long id) {
        Payment payment = find(id);
        if (payment.getStatus() != PaymentStatus.POSTED) {
            throw new BadRequestException("Payment is already cancelled");
        }
        LocalDate date = LocalDate.now().isBefore(payment.getPaymentDate()) ? payment.getPaymentDate() : LocalDate.now();
        journalEntryService.reverse(payment.getJournalEntry().getId(),
                new ReverseEntryRequest(date, "Cancellation of payment " + payment.getPaymentNumber()));
        Invoice invoice = invoiceService.find(payment.getInvoice().getId());
        invoice.removePayment(payment.getAmount());
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(LocalDateTime.now());
        payment.setCancelledBy(currentUser());
        log.info("Payment {} cancelled; {} now {}", payment.getPaymentNumber(), invoice.getInvoiceNumber(),
                invoice.getPaymentState());
        return mapper.toResponse(payment);
    }

    private Payment find(Long id) {
        return paymentRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    private User currentUser() {
        return SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
    }
}
