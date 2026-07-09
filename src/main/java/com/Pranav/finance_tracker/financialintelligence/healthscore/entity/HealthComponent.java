package com.Pranav.finance_tracker.financialintelligence.healthscore.entity;

/**
 * The contributing dimensions of a financial health score. Each maps to one
 * {@code HealthComponentCalculator}, so a component's logic can be replaced (e.g. by an ML model)
 * independently of the others.
 */
public enum HealthComponent {

    /** How well the user stays within their category budgets. */
    BUDGET_ADHERENCE,

    /** Whether the user saves regularly and consistently. */
    SAVINGS_BEHAVIOR,

    /** How much outstanding debt the user carries. */
    DEBT_MANAGEMENT,

    /** How stable spending is month-to-month (spikes reduce the score). */
    SPENDING_STABILITY
}
