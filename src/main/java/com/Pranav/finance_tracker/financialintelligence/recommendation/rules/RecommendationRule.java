package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;

import java.util.List;

/**
 * A single, self-contained piece of recommendation logic.
 *
 * <p>Each rule is its own Spring bean, discovered automatically by the {@code RecommendationEngine}.
 * Adding advice means adding a new implementation — no existing class changes (Open/Closed).</p>
 *
 * <p><b>Future compatibility:</b> the interface is the seam for smarter engines. A rule can be
 * reimplemented with machine learning, collaborative filtering, a user-behaviour model,
 * reinforcement learning or an LLM-generated explanation, and — as long as it still returns
 * {@link RecommendationDraft}s from a {@link RecommendationContext} — the engine, priority engine,
 * service, REST API and database schema are all unaffected.</p>
 */
public interface RecommendationRule {

    /** Stable identifier of this rule, used for logging and dedup key prefixes. */
    String ruleKey();

    /** The type of recommendation this rule produces. */
    RecommendationType type();

    /**
     * Evaluates the rule against the supplied context.
     *
     * @param context preloaded, health-scored data for one user
     * @return zero or more recommendation drafts (never {@code null})
     */
    List<RecommendationDraft> evaluate(RecommendationContext context);
}
