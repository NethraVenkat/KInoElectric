package com.backend.javaleap.repository;

import com.backend.javaleap.entity.AnalyticAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalyticAccountRepository extends JpaRepository<AnalyticAccount, Long> {

    Optional<AnalyticAccount> findByZoneId(Long zoneId);

    Optional<AnalyticAccount> findByCode(String code);

    boolean existsByCode(String code);

    List<AnalyticAccount> findByStationIdOrderByCodeAsc(Long stationId);
}
