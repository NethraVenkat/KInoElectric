package com.backend.javaleap.repository;

import com.backend.javaleap.entity.KineticConversionRate;
import com.backend.javaleap.entity.enums.TileType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KineticConversionRateRepository extends JpaRepository<KineticConversionRate, Long> {

    Optional<KineticConversionRate> findByTileType(TileType tileType);

    @EntityGraph(attributePaths = "updatedBy")
    List<KineticConversionRate> findAllByOrderByTileTypeAsc();
}
