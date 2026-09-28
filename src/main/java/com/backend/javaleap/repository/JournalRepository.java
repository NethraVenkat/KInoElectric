package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Journal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {

    @EntityGraph(attributePaths = "defaultAccount")
    Optional<Journal> findByCode(String code);

    @EntityGraph(attributePaths = "defaultAccount")
    List<Journal> findAllByOrderByCodeAsc();

    /** Row lock so two postings never get the same number. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Journal j where j.id = :id")
    Optional<Journal> findByIdForUpdate(@Param("id") Long id);
}
