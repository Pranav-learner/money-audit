package com.Pranav.finance_tracker.financialintelligence.recommendation.dto;

import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Immutable value object produced by a {@code RecommendationRule}.
 *
 * <p>A draft is a fully personalized recommendation (numbers already filled in) that has not yet
 * been persisted. The engine prioritizes and de-duplicates drafts before the service maps the
 * survivors onto {@code FinancialRecommendation} entities.</p>
 */
@Getter
@Builder
public class RecommendationDraft {

    /** Stable, possibly parameterized dedup key (e.g. {@code SUBSCRIPTION_OPTIMIZATION:Netflix}). */
    private final String ruleKey;

    private final String title;
    private final String description;
    private final RecommendationType recommendationType;

    /** Initial urgency assigned by the rule; the priority engine uses it as one ranking signal. */
    private final Priority priority;

    /** Estimated ₹/month the user could save or set aside; {@code null}/zero when not applicable. */
    private final BigDecimal expectedMonthlySaving;

    /** Confidence in [0.0, 1.0]. */
    private final double confidence;

    /** Short call-to-action, e.g. "Cancel subscription". */
    private final String actionText;
}
