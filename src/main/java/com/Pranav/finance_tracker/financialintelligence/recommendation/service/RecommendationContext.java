package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Immutable per-user data set for one recommendation run.
 *
 * <p>Composes the already-preloaded {@link InsightContext} (spending, budgets, debt, savings trend —
 * <b>reused, not re-queried</b>) with the extra signals recommendations need: the user's health
 * score and savings position. Building on {@code InsightContext} means the spending-intelligence,
 * risk and recommendation phases all share a single set of database reads.</p>
 */
@Getter
@Builder
public class RecommendationContext {

    /** The shared, preloaded financial context (source of all expense/budget/debt/savings data). */
    private final InsightContext insight;

    /** Financial health score in [0, 100] supplied by a {@link HealthScoreProvider}. */
    private final int healthScore;

    /** All-time total savings recorded by the user (₹). */
    private final BigDecimal totalSavings;

    /** Savings recorded in the current month (₹). */
    private final BigDecimal savedThisMonth;

    /** Convenience: total spend in the current calendar month. */
    public BigDecimal monthlySpend() {
        return insight.totalSpend(insight.getCurrentMonthExpenses());
    }
}
