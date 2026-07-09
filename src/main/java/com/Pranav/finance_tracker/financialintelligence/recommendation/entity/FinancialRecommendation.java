package com.Pranav.finance_tracker.financialintelligence.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single personalized, actionable financial recommendation produced by the Recommendation Engine.
 *
 * <p>Like {@code FinancialInsight}, it references its owner by {@code userId} only to keep the
 * module self-contained. Recommendations form an <b>append-only history</b>: a nightly run never
 * mutates prior rows — it either skips an already-present recommendation (same {@code ruleKey} that
 * day) or inserts a new one. User actions move a row through its {@link RecommendationStatus}.</p>
 */
@Entity
@Table(
        name = "financial_recommendations",
        indexes = {
                @Index(name = "idx_reco_user_status_created", columnList = "user_id, status, created_at"),
                @Index(name = "idx_reco_user_rule_created", columnList = "user_id, rule_key, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Stable, possibly parameterized identifier of the rule that produced this recommendation
     * (e.g. {@code SUBSCRIPTION_OPTIMIZATION:Netflix}). Used to avoid regenerating the same advice
     * twice in one day. Not exposed to clients.
     */
    @Column(name = "rule_key", nullable = false, length = 120)
    private String ruleKey;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false, length = 40)
    private RecommendationType recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    /** Estimated money (₹) the user could save or set aside each month by acting on this. */
    @Column(name = "expected_monthly_saving", precision = 15, scale = 2)
    private BigDecimal expectedMonthlySaving;

    /** Engine confidence in this recommendation, in [0.0, 1.0]. */
    @Column(nullable = false)
    private double confidence;

    /** Short call-to-action shown on the button/link. */
    @Column(name = "action_text", length = 200)
    private String actionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** After this instant an ACTIVE recommendation is considered stale. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
