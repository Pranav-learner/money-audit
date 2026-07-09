package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

/**
 * Supplies a 0–100 financial health score for a user, used by the recommendation priority engine.
 *
 * <p>This is an explicit seam. The Recommendation Engine "reuses the Health Score" through this
 * interface; the shipped {@link HeuristicHealthScoreProvider} derives a score from the already
 * preloaded {@link InsightContext} (budgets, savings, debt), and when a dedicated Financial Health
 * Score Engine is introduced it simply provides another {@code HealthScoreProvider} bean — no
 * recommendation rule, engine, API or schema changes.</p>
 */
public interface HealthScoreProvider {

    /**
     * @param context preloaded financial data for one user
     * @return a health score in the range [0, 100] (higher is healthier)
     */
    int scoreFor(InsightContext context);
}
