package com.Pranav.finance_tracker.financialintelligence.healthscore.repository;

import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.FinancialHealthScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Persistence for {@link FinancialHealthScore}, scoped by {@code userId}. Scores are append-only
 * history; reads take the most recent snapshot(s).
 */
@Repository
public interface FinancialHealthScoreRepository extends JpaRepository<FinancialHealthScore, UUID> {

    /** Idempotency guard for the nightly generator: has a score already been recorded today? */
    boolean existsByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    /** The latest score for the user, or {@code null} if none. */
    FinancialHealthScore findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    /** The most recent scores, newest first (use a {@link Pageable} to limit history depth). */
    List<FinancialHealthScore> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
