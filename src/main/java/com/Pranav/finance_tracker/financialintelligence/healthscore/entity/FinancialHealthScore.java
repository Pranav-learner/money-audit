package com.Pranav.finance_tracker.financialintelligence.healthscore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A persisted, explainable financial health score for a point in time.
 *
 * <p>References its owner by {@code userId} only. Scores are append-only history: each nightly run
 * inserts a new snapshot (de-duplicated per day) so the trend and "why did it change" can be derived
 * by comparing consecutive rows. Component sub-scores are stored so the breakdown is queryable.</p>
 */
@Entity
@Table(
        name = "financial_health_scores",
        indexes = {
                @Index(name = "idx_health_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Overall score in [0, 100]. */
    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthBand band;

    @Column(name = "budget_score", nullable = false)
    private int budgetScore;

    @Column(name = "savings_score", nullable = false)
    private int savingsScore;

    @Column(name = "debt_score", nullable = false)
    private int debtScore;

    @Column(name = "spending_score", nullable = false)
    private int spendingScore;

    /** Plain-language explanation of the score and its main drivers. */
    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
