package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.enums.ContactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    @EntityGraph(attributePaths = {"receivableAccount", "payableAccount"})
    Optional<Contact> findWithAccountsById(Long id);

    Optional<Contact> findByCode(String code);

    Optional<Contact> findByGstin(String gstin);

    /** Filterable list; {@code types} lets CUSTOMER searches also include BOTH. */
    @EntityGraph(attributePaths = {"receivableAccount", "payableAccount"})
    @Query("""
            select c from Contact c
            where c.contactType in :types
              and (:active is null or c.active = :active)
              and (:q is null or lower(c.name) like lower(concat('%', :q, '%'))
                   or lower(c.code) like lower(concat('%', :q, '%'))
                   or lower(c.gstin) like lower(concat('%', :q, '%')))
            """)
    Page<Contact> search(@Param("types") Collection<ContactType> types,
                         @Param("active") Boolean active,
                         @Param("q") String q,
                         Pageable pageable);

    /** Highest existing code with a prefix, e.g. CUST-0007 (used to generate the next code). */
    @Query("select max(c.code) from Contact c where c.code like concat(:prefix, '%')")
    String findMaxCode(@Param("prefix") String prefix);
}
