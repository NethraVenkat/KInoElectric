package com.backend.javaleap.repository;

import com.backend.javaleap.entity.JournalEntry;
import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.EntryStatus;
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
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /** Entry with all lines, accounts, contacts and analytic accounts loaded. */
    @EntityGraph(attributePaths = {"journal", "items", "items.account", "items.contact", "items.analyticAccount",
            "reversalOf", "createdBy", "postedBy"})
    Optional<JournalEntry> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"journal", "createdBy", "postedBy"})
    @Query("""
            select e from JournalEntry e
            where (:journalCode is null or e.journal.code = :journalCode)
              and (:status is null or e.status = :status)
              and (:sourceType is null or e.sourceType = :sourceType)
              and (:from is null or e.entryDate >= :from)
              and (:to is null or e.entryDate <= :to)
              and (:q is null or lower(e.entryNumber) like lower(concat('%', :q, '%'))
                   or lower(e.reference) like lower(concat('%', :q, '%'))
                   or lower(e.narration) like lower(concat('%', :q, '%')))
            """)
    Page<JournalEntry> search(@Param("journalCode") String journalCode,
                              @Param("status") EntryStatus status,
                              @Param("sourceType") EntrySourceType sourceType,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              @Param("q") String q,
                              Pageable pageable);

    boolean existsByReversalOfId(Long id);

    List<JournalEntry> findBySourceTypeAndSourceId(EntrySourceType sourceType, Long sourceId);

    long countByStatus(EntryStatus status);
}
