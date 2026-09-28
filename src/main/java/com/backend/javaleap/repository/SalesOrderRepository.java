package com.backend.javaleap.repository;

import com.backend.javaleap.entity.SalesOrder;
import com.backend.javaleap.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @EntityGraph(attributePaths = {"customer", "customer.receivableAccount", "station", "lines", "lines.product",
            "lines.product.incomeAccount", "lines.analyticAccount", "createdBy"})
    Optional<SalesOrder> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"customer", "station", "createdBy"})
    @Query("""
            select s from SalesOrder s
            where (:status is null or s.status = :status)
              and (:customerId is null or s.customer.id = :customerId)
              and (:q is null or lower(s.soNumber) like lower(concat('%', :q, '%'))
                   or lower(s.reference) like lower(concat('%', :q, '%'))
                   or lower(s.customer.name) like lower(concat('%', :q, '%')))
            """)
    Page<SalesOrder> search(@Param("status") OrderStatus status, @Param("customerId") Long customerId,
                            @Param("q") String q, Pageable pageable);

    /** Prevents billing the same station's energy twice for overlapping periods. */
    @Query("""
            select count(s) > 0 from SalesOrder s
            where s.station.id = :stationId
              and s.status <> com.backend.javaleap.entity.enums.OrderStatus.CANCELLED
              and s.periodStart <= :periodEnd and s.periodEnd >= :periodStart
            """)
    boolean existsEnergyOrderOverlapping(@Param("stationId") Long stationId,
                                         @Param("periodStart") LocalDate periodStart,
                                         @Param("periodEnd") LocalDate periodEnd);
}
