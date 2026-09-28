package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.JournalItem;
import com.backend.javaleap.entity.enums.EntryStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.response.LedgerLineResponse;
import com.backend.javaleap.model.response.LedgerResponse;
import com.backend.javaleap.model.response.TrialBalanceResponse;
import com.backend.javaleap.model.response.TrialBalanceRow;
import com.backend.javaleap.repository.AccountRepository;
import com.backend.javaleap.repository.JournalItemRepository;
import com.backend.javaleap.repository.projection.AccountBalanceView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Trial balance and general ledger from POSTED (and REVERSED) journal entries. */
@Service
@RequiredArgsConstructor
public class LedgerService {

    /** A reversed entry and its reversal both stay in the books; together they net to zero. */
    public static final Set<EntryStatus> BOOKED = EnumSet.of(EntryStatus.POSTED, EntryStatus.REVERSED);

    private final JournalItemRepository itemRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public TrialBalanceResponse trialBalance(LocalDate asOf) {
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        List<TrialBalanceRow> rows = new ArrayList<>();
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (AccountBalanceView v : itemRepository.balancesAsOf(BOOKED, date)) {
            BigDecimal net = v.getDebit().subtract(v.getCredit());
            BigDecimal drBal = net.signum() > 0 ? net : BigDecimal.ZERO;
            BigDecimal crBal = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            if (v.getDebit().signum() == 0 && v.getCredit().signum() == 0) {
                continue;
            }
            rows.add(new TrialBalanceRow(v.getCode(), v.getName(), v.getAccountType(), v.getDebit(), v.getCredit(),
                    drBal, crBal));
            totalDr = totalDr.add(drBal);
            totalCr = totalCr.add(crBal);
        }
        return new TrialBalanceResponse(date, rows, totalDr, totalCr, totalDr.compareTo(totalCr) == 0);
    }

    @Transactional(readOnly = true)
    public LedgerResponse ledger(Long accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfYear(1) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be on or before to");
        }
        BigDecimal opening = itemRepository.balanceBefore(accountId, BOOKED, start);
        BigDecimal running = opening;
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        List<LedgerLineResponse> lines = new ArrayList<>();
        for (JournalItem i : itemRepository.ledgerLines(accountId, BOOKED, start, end)) {
            running = running.add(i.getDebit()).subtract(i.getCredit());
            totalDr = totalDr.add(i.getDebit());
            totalCr = totalCr.add(i.getCredit());
            lines.add(new LedgerLineResponse(i.getEntry().getEntryDate(), i.getEntry().getId(),
                    i.getEntry().getEntryNumber(), i.getEntry().getJournal().getCode(), i.getEntry().getReference(),
                    i.getLabel(), i.getContact() == null ? null : i.getContact().getName(),
                    i.getDebit(), i.getCredit(), running));
        }
        return new LedgerResponse(account.getCode(), account.getName(), start, end, opening, totalDr, totalCr,
                running, lines);
    }
}
