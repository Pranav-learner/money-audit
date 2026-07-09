package com.Pranav.finance_tracker.financialintelligence.dto;

import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable value object produced by an {@code InsightRule}.
 *
 * <p>A draft is a fully-built recommendation (title, description and suggestion already
 * personalized with real numbers) that has not yet been persisted. The service layer
 * de-duplicates drafts and maps surviving ones onto {@code FinancialInsight} entities.</p>
 */
@Getter
@Builder
public class InsightDraft {

    /**
     * Stable, possibly parameterized dedup key (e.g. {@code BUDGET_USAGE:Food}).
     * Two drafts sharing a key are considered "the same insight" for a given day.
     */
    private final String ruleKey;

    private final String title;
    private final String description;
    private final InsightType insightType;
    private final Severity severity;

    /**
     * Risk classification when this draft was produced by the Risk Detection Engine (Module 2),
     * or {@code null} for a plain spending-intelligence insight (Module 1).
     */
    private final FinancialRiskType riskType;

    /** Optional business category the insight relates to. */
    private final String category;

    private final String actionSuggestion;

    /** Confidence in [0.0, 1.0]. */
    private final double confidence;
}
