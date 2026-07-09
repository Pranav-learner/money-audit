package com.Pranav.finance_tracker.financialintelligence.repository;

import com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link FinancialInsight}. All queries are scoped by {@code userId}
 * so a user can only ever see their own insights.
 */
@Repository
public interface FinancialInsightRepository extends JpaRepository<FinancialInsight, UUID> {

    /**
     * Idempotency guard for the nightly generator: returns {@code true} if the given rule
     * already produced an insight for this user within the supplied window (typically "today").
     */
    boolean existsByUserIdAndRuleKeyAndCreatedAtBetween(
            UUID userId, String ruleKey, LocalDateTime start, LocalDateTime end);

    /** Active = not dismissed and not yet expired. Newest first. */
    List<FinancialInsight> findByUserIdAndDismissedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime now);

    /** Active and not yet viewed. Newest first. */
    List<FinancialInsight> findByUserIdAndDismissedFalseAndViewedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime now);

    /** Ownership-safe single fetch used by read / dismiss endpoints. */
    Optional<FinancialInsight> findByIdAndUserId(UUID id, UUID userId);
}
