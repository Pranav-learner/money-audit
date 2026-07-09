package com.Pranav.finance_tracker.financialintelligence.entity;

/**
 * Classifies the nature of a generated {@link FinancialInsight}.
 * The frontend uses this to pick icons, colours and grouping on the AI Insights page.
 */
public enum InsightType {

    /** A concrete opportunity to reduce spending / increase savings. */
    SAVING_OPPORTUNITY,

    /** Spending in a category has grown in a way worth flagging. */
    SPENDING_WARNING,

    /** A budget is near or over its limit. */
    BUDGET_ALERT,

    /** A behavioural pattern detected over time (e.g. weekend spending). */
    TREND,

    /** Positive reinforcement for good financial behaviour. */
    ACHIEVEMENT,

    /** Neutral, informational nudge (e.g. no savings recorded recently). */
    INFORMATION
}
