package com.Pranav.finance_tracker.financialintelligence.forecast.repository;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialGoal;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link FinancialGoal}, scoped by {@code userId}.
 */
@Repository
public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

    List<FinancialGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FinancialGoal> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, GoalStatus status);

    Optional<FinancialGoal> findByIdAndUserId(UUID id, UUID userId);
}
