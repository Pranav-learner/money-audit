package com.Pranav.finance_tracker.financialintelligence.healthscore;

import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

/**
 * Supplies a 0–100 financial health score for a user.
 *
 * <p>This is the seam other engines (recommendations, forecasting) depend on when they need a single
 * numeric health score without the full breakdown. It is owned by the Financial Health Score Engine
 * (Module 3) and backed by {@code HealthScoreEngine}; a future ML scorer can replace the bean without
 * affecting any consumer.</p>
 */
public interface HealthScoreProvider {

    /**
     * @param context preloaded financial data for one user
     * @return a health score in the range [0, 100] (higher is healthier)
     */
    int scoreFor(InsightContext context);
}
