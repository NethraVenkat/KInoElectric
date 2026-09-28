package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Product;
import com.backend.javaleap.entity.enums.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"incomeAccount", "expenseAccount"})
    Optional<Product> findWithAccountsById(Long id);

    @EntityGraph(attributePaths = {"incomeAccount", "expenseAccount"})
    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"incomeAccount", "expenseAccount"})
    @Query("""
            select p from Product p
            where (:type is null or p.productType = :type)
              and (:sold is null or p.canBeSold = :sold)
              and (:purchased is null or p.canBePurchased = :purchased)
              and (:active is null or p.active = :active)
              and (:q is null or lower(p.name) like lower(concat('%', :q, '%'))
                   or lower(p.code) like lower(concat('%', :q, '%')))
            """)
    Page<Product> search(@Param("type") ProductType type,
                         @Param("sold") Boolean sold,
                         @Param("purchased") Boolean purchased,
                         @Param("active") Boolean active,
                         @Param("q") String q,
                         Pageable pageable);
}
