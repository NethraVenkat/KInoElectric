package com.backend.javaleap.repository;

import com.backend.javaleap.entity.ApiClient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<ApiClient> findByKeyHash(String keyHash);

    @EntityGraph(attributePaths = "user")
    List<ApiClient> findAllByOrderByCreatedAtDesc();
}
