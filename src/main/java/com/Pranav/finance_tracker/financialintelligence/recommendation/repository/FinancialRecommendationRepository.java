package com.Pranav.finance_tracker.financialintelligence.recommendation.repository;

import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.FinancialRecommendation;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link FinancialRecommendation}. All queries are scoped by {@code userId} so a
 * user only ever sees their own recommendations. The table is append-only history, so reads filter
 * by {@link RecommendationStatus} and expiry rather than deleting rows.
 */
@Repository
public interface FinancialRecommendationRepository extends JpaRepository<FinancialRecommendation, UUID> {

    /** Idempotency guard for the nightly generator: has this rule already fired today for the user? */
    boolean existsByUserIdAndRuleKeyAndCreatedAtBetween(
            UUID userId, String ruleKey, LocalDateTime start, LocalDateTime end);

    /** Active (live, unexpired) recommendations, newest first. */
    List<FinancialRecommendation> findByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, RecommendationStatus status, LocalDateTime now);

    /** Full history regardless of status, newest first. */
    List<FinancialRecommendation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Recommendations in a given status, newest first (e.g. recently completed). */
    List<FinancialRecommendation> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, RecommendationStatus status);

    /** Count by status for summary tiles. */
    long countByUserIdAndStatus(UUID userId, RecommendationStatus status);

    /** Ownership-safe single fetch used by the dismiss / complete endpoints. */
    Optional<FinancialRecommendation> findByIdAndUserId(UUID id, UUID userId);
}
