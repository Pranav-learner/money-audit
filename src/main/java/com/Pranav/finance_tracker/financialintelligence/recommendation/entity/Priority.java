package com.Pranav.finance_tracker.financialintelligence.recommendation.entity;

/**
 * Relative urgency of a {@link FinancialRecommendation}. Ordinal order (LOW &lt; … &lt; CRITICAL) is
 * meaningful and is used by the priority engine and for sorting.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
