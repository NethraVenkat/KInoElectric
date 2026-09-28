package com.backend.javaleap.repository;

import com.backend.javaleap.entity.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, String> {
}
