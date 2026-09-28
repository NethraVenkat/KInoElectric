package com.backend.javaleap.repository;

import com.backend.javaleap.entity.JournalItem;
import com.backend.javaleap.entity.enums.EntryStatus;
import com.backend.javaleap.repository.projection.AccountBalanceView;
import com.backend.javaleap.repository.projection.AnalyticActualView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface JournalItemRepository extends JpaRepository<JournalItem, Long> {

    boolean existsByAccountId(Long accountId);

    boolean existsByContactId(Long contactId);

    /** Posted totals per account up to a date (trial balance). REVERSED entries still count - their reversal nets them out. */
    @Query("""
            select a.id as accountId, a.code as code, a.name as name,
                   a.accountType as accountType, a.subType as subType,
                   coalesce(sum(i.debit), 0) as debit, coalesce(sum(i.credit), 0) as credit
            from JournalItem i
            join i.account a
            join i.entry e
            where e.status in :statuses and e.entryDate <= :asOf
            group by a.id, a.code, a.name, a.accountType, a.subType
            order by a.code
            """)
    List<AccountBalanceView> balancesAsOf(@Param("statuses") Collection<EntryStatus> statuses,
                                          @Param("asOf") LocalDate asOf);

    /** Posted totals per account inside a period (profit and loss). */
    @Query("""
            select a.id as accountId, a.code as code, a.name as name,
                   a.accountType as accountType, a.subType as subType,
                   coalesce(sum(i.debit), 0) as debit, coalesce(sum(i.credit), 0) as credit
            from JournalItem i
            join i.account a
            join i.entry e
            where e.status in :statuses and e.entryDate between :from and :to
            group by a.id, a.code, a.name, a.accountType, a.subType
            order by a.code
            """)
    List<AccountBalanceView> balancesBetween(@Param("statuses") Collection<EntryStatus> statuses,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    /** Ledger lines of one account in a period. */
    @Query("""
            select i from JournalItem i
            join fetch i.entry e
            join fetch e.journal
            left join fetch i.contact
            where i.account.id = :accountId and e.status in :statuses
              and e.entryDate between :from and :to
            order by e.entryDate, e.id, i.lineNo
            """)
    List<JournalItem> ledgerLines(@Param("accountId") Long accountId,
                                  @Param("statuses") Collection<EntryStatus> statuses,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    /** Debit minus credit of an account before a date (ledger opening balance). */
    @Query("""
            select coalesce(sum(i.debit), 0) - coalesce(sum(i.credit), 0)
            from JournalItem i join i.entry e
            where i.account.id = :accountId and e.status in :statuses and e.entryDate < :before
            """)
    BigDecimal balanceBefore(@Param("accountId") Long accountId,
                             @Param("statuses") Collection<EntryStatus> statuses,
                             @Param("before") LocalDate before);

    /** Posted totals per analytic account (zone) and account inside a period (budget actuals). */
    @Query("""
            select aa.id as analyticAccountId, a.id as accountId,
                   coalesce(sum(i.debit), 0) as debit, coalesce(sum(i.credit), 0) as credit
            from JournalItem i
            join i.analyticAccount aa
            join i.account a
            join i.entry e
            where e.status in :statuses and e.entryDate between :from and :to
            group by aa.id, a.id
            """)
    List<AnalyticActualView> analyticActuals(@Param("statuses") Collection<EntryStatus> statuses,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);
}
