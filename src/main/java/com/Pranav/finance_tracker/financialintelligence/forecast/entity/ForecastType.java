package com.Pranav.finance_tracker.financialintelligence.forecast.entity;

/**
 * The quantity a {@link FinancialForecast} predicts. Each value maps to exactly one predictor,
 * so a future ML model can replace a rule-based predictor while keeping the same type.
 */
public enum ForecastType {

    /** Projected total spend at month end. */
    MONTHLY_SPENDING,

    /** Projected savings for the month. */
    MONTHLY_SAVINGS,

    /** Projected month-end cash position (inflow minus projected spend). */
    CASHFLOW,

    /** Projected budget usage (whether/by how much the overall budget will be exceeded). */
    BUDGET_USAGE,

    /** Projected outstanding debt if current behaviour continues. */
    DEBT,

    /** Projected net worth (savings minus debt) trend. */
    NET_WORTH,

    /** Projected completion outlook for a financial goal. */
    GOAL_COMPLETION
}
