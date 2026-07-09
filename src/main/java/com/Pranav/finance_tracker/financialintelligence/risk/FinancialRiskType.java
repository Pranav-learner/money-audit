package com.Pranav.finance_tracker.financialintelligence.risk;

/**
 * Classifies a financial risk detected by the Risk Detection Engine (Module 2).
 *
 * <p>Every risk-oriented {@link com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft}
 * carries one of these values, which is persisted on the resulting
 * {@link com.Pranav.finance_tracker.financialintelligence.entity.FinancialInsight} and surfaced
 * through the existing REST API. The frontend uses it to group, colour and filter risk alerts.</p>
 *
 * <p>The enum is intentionally decoupled from the concrete rule that produced it: a future
 * ML model (e.g. {@code BudgetRiskMLModel}) can emit the same {@code BUDGET_RISK} type without
 * any change to controllers, DTOs or the database schema.</p>
 */
public enum FinancialRiskType {

    /** A category budget is near or over its limit. */
    BUDGET_RISK,

    /** Projected month-end spending threatens to exceed the available budget. */
    CASHFLOW_RISK,

    /** Outstanding debt / unsettled balances have grown to a concerning level. */
    DEBT_RISK,

    /** Savings contributions have stalled or are trending downwards. */
    SAVINGS_RISK,

    /** A recurring subscription-like charge looks inactive or expensive. */
    SUBSCRIPTION_RISK,

    /** Short-term spending has spiked well above the recent baseline. */
    SPENDING_SPIKE,

    /** A recurring bill (rent, EMI, insurance, …) is due soon. */
    RECURRING_PAYMENT,

    /** A statistically unusual transaction or pattern was detected. */
    UNUSUAL_ACTIVITY
}
