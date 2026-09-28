package com.backend.javaleap.repository;

import com.backend.javaleap.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    boolean existsByCode(String code);

    List<Budget> findAllByOrderByDateFromDescIdDesc();
}
