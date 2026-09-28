package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Journal;
import com.backend.javaleap.entity.JournalEntry;
import com.backend.javaleap.entity.JournalItem;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.EntryStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.JournalEntryRequest;
import com.backend.javaleap.model.request.JournalLineRequest;
import com.backend.javaleap.model.request.ReverseEntryRequest;
import com.backend.javaleap.model.response.JournalEntryResponse;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.JournalEntryRepository;
import com.backend.javaleap.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * The double-entry engine.
 * <ul>
 *   <li>Every entry must balance (total debit = total credit, &gt; 0).</li>
 *   <li>Each line is either a debit or a credit on an active posting account.</li>
 *   <li>Receivable / payable lines must name a contact (needed for customer / vendor balances).</li>
 *   <li>Posting assigns a gap-free number per journal and year, under a row lock.</li>
 *   <li>Posted entries are immutable; corrections are made by reversal entries.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalEntryService {

    private static final EnumSet<AccountSubType> CONTACT_REQUIRED =
            EnumSet.of(AccountSubType.RECEIVABLE, AccountSubType.PAYABLE);

    private final JournalEntryRepository entryRepository;
    private final EntityManager entityManager;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final UserRepository userRepository;
    private final JournalService journalService;
    private final AccountService accountService;
    private final ContactService contactService;
    private final AccountingMapper mapper;

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PageResponse<JournalEntryResponse> search(String journalCode, EntryStatus status, EntrySourceType sourceType,
                                                     LocalDate from, LocalDate to, String q, Pageable pageable) {
        return PageResponse.from(entryRepository.search(
                journalCode == null || journalCode.isBlank() ? null : journalCode.trim().toUpperCase(Locale.ROOT),
                status, sourceType, from, to, q == null || q.isBlank() ? null : q.trim(), pageable),
                mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse get(Long id) {
        return mapper.toDetail(findDetailed(id));
    }

    // ------------------------------------------------------------------ manual entries

    @Transactional
    public JournalEntryResponse createDraft(JournalEntryRequest req) {
        JournalEntry entry = JournalEntry.builder()
                .journal(journalService.require(req.journalCode()))
                .entryDate(checkDate(req.entryDate()))
                .reference(trim(req.reference()))
                .narration(trim(req.narration()))
                .status(EntryStatus.DRAFT)
                .sourceType(EntrySourceType.MANUAL)
                .createdBy(currentUser())
                .build();
        entry.replaceItems(resolveLines(req.lines()));
        requireBalanced(entry);
        JournalEntry saved = entryRepository.save(entry);
        log.info("Draft journal entry {} created in {} ({} {})", saved.getId(), saved.getJournal().getCode(),
                saved.getTotalDebit(), "INR");
        return mapper.toDetail(saved);
    }

    @Transactional
    public JournalEntryResponse updateDraft(Long id, JournalEntryRequest req) {
        JournalEntry entry = findDetailed(id);
        requireDraft(entry);
        entry.setJournal(journalService.require(req.journalCode()));
        entry.setEntryDate(checkDate(req.entryDate()));
        entry.setReference(trim(req.reference()));
        entry.setNarration(trim(req.narration()));
        entry.replaceItems(resolveLines(req.lines()));
        requireBalanced(entry);
        return mapper.toDetail(entry);
    }

    @Transactional
    public void deleteDraft(Long id) {
        JournalEntry entry = findDetailed(id);
        requireDraft(entry);
        entryRepository.delete(entry);
        log.info("Draft journal entry {} deleted", id);
    }

    /** Cancels a draft but keeps it for audit. Posted entries must be reversed instead. */
    @Transactional
    public JournalEntryResponse cancelDraft(Long id) {
        JournalEntry entry = findDetailed(id);
        requireDraft(entry);
        entry.setStatus(EntryStatus.CANCELLED);
        entry.setCancelledAt(LocalDateTime.now());
        entry.setCancelledBy(currentUser());
        return mapper.toDetail(entry);
    }

    @Transactional
    public JournalEntryResponse post(Long id) {
        JournalEntry entry = findDetailed(id);
        requireDraft(entry);
        doPost(entry);
        return mapper.toDetail(entry);
    }

    /**
     * Reverses a posted entry: creates and posts a mirror entry (debits and credits swapped)
     * and marks the original REVERSED. Net effect on every account is zero.
     */
    @Transactional
    public JournalEntryResponse reverse(Long id, ReverseEntryRequest req) {
        JournalEntry original = findDetailed(id);
        if (original.getStatus() != EntryStatus.POSTED) {
            throw new BadRequestException("Only POSTED entries can be reversed (current: " + original.getStatus() + ")");
        }
        if (entryRepository.existsByReversalOfId(id)) {
            throw new BadRequestException("Entry " + original.getEntryNumber() + " is already reversed");
        }
        LocalDate date = req.reversalDate() == null ? LocalDate.now() : req.reversalDate();
        if (date.isBefore(original.getEntryDate())) {
            throw new BadRequestException("Reversal date cannot be before the original entry date");
        }
        JournalEntry reversal = JournalEntry.builder()
                .journal(original.getJournal())
                .entryDate(checkDate(date))
                .reference("REV " + original.getEntryNumber())
                .narration("Reversal of " + original.getEntryNumber() + ": " + req.reason().trim())
                .status(EntryStatus.DRAFT)
                .sourceType(EntrySourceType.REVERSAL)
                .sourceId(original.getId())
                .reversalOf(original)
                .createdBy(currentUser())
                .build();
        reversal.replaceItems(original.getItems().stream().map(JournalItem::reversed).toList());
        entryRepository.save(reversal);
        doPost(reversal);
        original.setStatus(EntryStatus.REVERSED);
        log.info("Entry {} reversed by {}", original.getEntryNumber(), reversal.getEntryNumber());
        return mapper.toDetail(reversal);
    }

    // ------------------------------------------------------------------ system postings (Phase D)

    /**
     * Creates and immediately posts a system-generated entry (customer invoice, vendor bill, payment).
     * Runs inside the caller's transaction.
     */
    @Transactional
    public JournalEntry createPosted(String journalCode, LocalDate date, String reference, String narration,
                                     EntrySourceType sourceType, Long sourceId, List<PostingLine> lines) {
        JournalEntry entry = JournalEntry.builder()
                .journal(journalService.require(journalCode))
                .entryDate(checkDate(date))
                .reference(trim(reference))
                .narration(trim(narration))
                .status(EntryStatus.DRAFT)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .createdBy(currentUser())
                .build();
        List<JournalItem> items = new ArrayList<>();
        for (PostingLine l : lines) {
            items.add(validatedItem(l.account(), l.contact(), l.analyticAccount(), l.label(), l.debit(), l.credit()));
        }
        entry.replaceItems(items);
        requireBalanced(entry);
        entryRepository.save(entry);
        doPost(entry);
        return entry;
    }

    // ------------------------------------------------------------------ internals

    private void doPost(JournalEntry entry) {
        requireBalanced(entry);
        // refresh + row lock: reloads the latest counter even if the journal is already in this session
        Journal journal = entry.getJournal();
        entityManager.refresh(journal, LockModeType.PESSIMISTIC_WRITE);
        entry.setEntryNumber(journal.takeNextNumber(entry.getEntryDate()));
        entry.setStatus(EntryStatus.POSTED);
        entry.setPostedAt(LocalDateTime.now());
        entry.setPostedBy(currentUser());
        log.info("Posted {} ({} {}, {} lines)", entry.getEntryNumber(), entry.getTotalDebit(), "INR",
                entry.getItems().size());
    }

    private List<JournalItem> resolveLines(List<JournalLineRequest> lines) {
        List<JournalItem> items = new ArrayList<>();
        int n = 1;
        for (JournalLineRequest l : lines) {
            try {
                Account account = accountService.requirePostable(l.accountCode());
                Contact contact = l.contactId() == null ? null : contactService.requireActive(l.contactId());
                AnalyticAccount analytic = null;
                if (l.analyticAccountCode() != null && !l.analyticAccountCode().isBlank()) {
                    analytic = analyticAccountRepository.findByCode(l.analyticAccountCode().trim())
                            .filter(a -> Boolean.TRUE.equals(a.getActive()))
                            .orElseThrow(() -> new BadRequestException(
                                    "Unknown or inactive analytic account " + l.analyticAccountCode()));
                }
                items.add(validatedItem(account, contact, analytic, trim(l.label()), l.debit(), l.credit()));
            } catch (BadRequestException ex) {
                throw new BadRequestException("Line " + n + ": " + ex.getMessage());
            }
            n++;
        }
        return items;
    }

    private JournalItem validatedItem(Account account, Contact contact, AnalyticAccount analytic, String label,
                                      BigDecimal debit, BigDecimal credit) {
        if (!account.isPostable()) {
            throw new BadRequestException("Account " + account.getCode() + " cannot be posted to");
        }
        BigDecimal dr = money(debit);
        BigDecimal cr = money(credit);
        if (dr.signum() < 0 || cr.signum() < 0) {
            throw new BadRequestException("Amounts cannot be negative");
        }
        if ((dr.signum() > 0) == (cr.signum() > 0)) {
            throw new BadRequestException("Each line needs either a debit or a credit amount (not both, not neither)");
        }
        if (CONTACT_REQUIRED.contains(account.getSubType()) && contact == null) {
            throw new BadRequestException("Account " + account.getCode() + " (" + account.getSubType()
                    + ") requires a contact");
        }
        return JournalItem.builder().account(account).contact(contact).analyticAccount(analytic)
                .label(label).debit(dr).credit(cr).build();
    }

    private static void requireBalanced(JournalEntry entry) {
        if (!entry.isBalanced()) {
            throw new BadRequestException("Entry is not balanced: debit " + entry.getTotalDebit()
                    + " vs credit " + entry.getTotalCredit());
        }
    }

    private static void requireDraft(JournalEntry entry) {
        if (!entry.isDraft()) {
            throw new BadRequestException("Entry " + (entry.getEntryNumber() == null ? entry.getId()
                    : entry.getEntryNumber()) + " is " + entry.getStatus() + " and cannot be changed");
        }
    }

    private static LocalDate checkDate(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("entryDate is required");
        }
        if (date.isAfter(LocalDate.now().plusYears(1))) {
            throw new BadRequestException("entryDate is more than a year in the future");
        }
        return date;
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private User currentUser() {
        return SecurityUtils.currentUserId().flatMap(userRepository::findById).orElse(null);
    }

    private JournalEntry findDetailed(Long id) {
        return entryRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("Journal entry", id));
    }
}
