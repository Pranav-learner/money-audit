package com.Pranav.finance_tracker.aiassistant.orchestrator;

/**
 * The categories of question the AI Financial Assistant understands. The {@code IntentRouter}
 * classifies each user message into one of these, which in turn selects the backend tools to run.
 */
public enum Intent {
    EXPENSE_SUMMARY,
    BUDGET_ANALYSIS,
    SAVINGS_ANALYSIS,
    FINANCIAL_HEALTH,
    RISK_ANALYSIS,
    RECOMMENDATION,
    FORECAST,
    GOAL_PLANNING,
    SPLITWISE,
    GENERAL_FINANCE,
    UNKNOWN
}
