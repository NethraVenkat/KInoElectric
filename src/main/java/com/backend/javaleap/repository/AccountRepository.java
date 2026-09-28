package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.enums.AccountSubType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "parent")
    Optional<Account> findByCode(String code);

    @EntityGraph(attributePaths = "parent")
    Optional<Account> findWithParentById(Long id);

    boolean existsByCode(String code);

    boolean existsByParentId(Long parentId);

    @EntityGraph(attributePaths = "parent")
    List<Account> findAllByOrderByCodeAsc();

    /** First active posting account of a role (default receivable, payable, revenue...). */
    Optional<Account> findFirstBySubTypeAndActiveTrueAndGroupAccountFalseOrderByCodeAsc(AccountSubType subType);
}
