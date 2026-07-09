package com.Pranav.finance_tracker.financialintelligence.recommendation.entity;

/**
 * Lifecycle state of a {@link FinancialRecommendation}.
 *
 * <p>Rows are <b>never overwritten or deleted</b> — status transitions are the audit trail that
 * future analytics (accept/dismiss/complete rates) will read from.</p>
 */
public enum RecommendationStatus {

    /** Live and shown to the user. */
    ACTIVE,

    /** The user acted on the advice. */
    COMPLETED,

    /** The user explicitly dismissed it. */
    DISMISSED,

    /** It passed its expiry without being acted on. */
    EXPIRED
}
