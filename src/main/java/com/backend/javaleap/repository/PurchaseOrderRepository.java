package com.backend.javaleap.repository;

import com.backend.javaleap.entity.PurchaseOrder;
import com.backend.javaleap.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"vendor", "vendor.payableAccount", "analyticAccount", "lines", "lines.product",
            "lines.product.expenseAccount", "lines.tile", "lines.analyticAccount", "createdBy"})
    Optional<PurchaseOrder> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"vendor", "createdBy"})
    @Query("""
            select p from PurchaseOrder p
            where (:status is null or p.status = :status)
              and (:vendorId is null or p.vendor.id = :vendorId)
              and (:q is null or lower(p.poNumber) like lower(concat('%', :q, '%'))
                   or lower(p.reference) like lower(concat('%', :q, '%'))
                   or lower(p.vendor.name) like lower(concat('%', :q, '%')))
            """)
    Page<PurchaseOrder> search(@Param("status") OrderStatus status, @Param("vendorId") Long vendorId,
                               @Param("q") String q, Pageable pageable);
}
