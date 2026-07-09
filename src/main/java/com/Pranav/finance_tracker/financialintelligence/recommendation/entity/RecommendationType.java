package com.Pranav.finance_tracker.financialintelligence.recommendation.entity;

/**
 * The domain a {@link FinancialRecommendation} addresses. The frontend uses this to group, icon
 * and colour recommendations; analytics uses it to measure which kinds of advice users act on.
 */
public enum RecommendationType {

    /** Increase or start regular saving. */
    SAVING,

    /** Adjust a category budget allocation. */
    BUDGET,

    /** Reduce or prioritise outstanding debt. */
    DEBT,

    /** Cancel or downgrade a recurring subscription. */
    SUBSCRIPTION,

    /** Reduce discretionary spending. */
    SPENDING,

    /** Put idle money to work (reserved for future investment advice). */
    INVESTMENT,

    /** Progress toward a financial goal (e.g. emergency fund). */
    GOAL,

    /** Change a spending habit (e.g. weekend spending). */
    HABIT
}
