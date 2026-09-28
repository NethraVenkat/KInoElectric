package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Battery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatteryRepository extends JpaRepository<Battery, Long> {

    List<Battery> findByStationIdOrderByCodeAsc(Long stationId);

    Optional<Battery> findFirstByStationIdOrderByIdAsc(Long stationId);

    Optional<Battery> findByCode(String code);
}
