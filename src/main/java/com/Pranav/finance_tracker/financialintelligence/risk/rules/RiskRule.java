package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightRule;

/**
 * A single financial-risk detector.
 *
 * <p>A {@code RiskRule} <b>is-an</b> {@link InsightRule} — it reuses the exact same evaluation
 * contract ({@code evaluate(InsightContext) -> List<InsightDraft>}) and the shared
 * {@link com.Pranav.finance_tracker.financialintelligence.rules.InsightContext}, so no parallel
 * rule architecture is introduced. It merely adds the {@link FinancialRiskType} it specialises in.</p>
 *
 * <p>Risk rules are discovered and executed by the
 * {@link com.Pranav.finance_tracker.financialintelligence.risk.service.RiskDetectionEngine} rather
 * than the base {@code InsightEngine} (which deliberately excludes {@code RiskRule} beans), keeping
 * the "Spending Intelligence → Risk Detection" phases distinct while sharing everything downstream
 * (dedup, persistence, notification).</p>
 *
 * <p><b>Future ML compatibility:</b> replacing a heuristic rule with a model is a drop-in — a new
 * bean implementing {@code RiskRule} (e.g. {@code BudgetRiskMLModel}) is picked up automatically
 * with no change to the engine, service, controller, DTOs or schema (Open/Closed Principle).</p>
 */
public interface RiskRule extends InsightRule {

    /**
     * The category of risk this rule detects. Every draft the rule emits is tagged with this
     * type by convention, giving the API and frontend a stable, model-agnostic classification.
     */
    FinancialRiskType riskType();
}
