package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Risk Rule 1 — Budget Risk.
 *
 * <p>Raises a portfolio-level budget-risk alert when one or more category budgets cross the
 * configured warning ({@code >= 80%}) or over-limit ({@code >= 100%}) thresholds. Severity is HIGH
 * when any budget is exceeded, otherwise MEDIUM.</p>
 *
 * <p>This intentionally produces a single <b>aggregate</b> risk signal (with the most-strained
 * category named), complementing — rather than duplicating — the Module&nbsp;1 per-category
 * {@code BudgetUsageRule}. Thresholds are externally configurable via {@link RiskThresholdProperties}.</p>
 */
@Component
@RequiredArgsConstructor
public class BudgetRiskRule implements RiskRule {

    private static final String RULE_KEY = "BUDGET_RISK";

    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.BUDGET_RISK;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<BudgetUsageResponse> usages = context.getBudgetUsages();
        if (usages == null || usages.isEmpty()) {
            return List.of();
        }

        List<BudgetUsageResponse> atRisk = usages.stream()
                .filter(u -> u.getPercentageUsed() >= thresholds.getBudgetWarnPercent())
                .sorted(Comparator.comparingInt(BudgetUsageResponse::getPercentageUsed).reversed())
                .toList();
        if (atRisk.isEmpty()) {
            return List.of();
        }

        BudgetUsageResponse worst = atRisk.get(0);
        boolean anyOver = worst.getPercentageUsed() >= thresholds.getBudgetOverPercent();
        Severity severity = anyOver ? Severity.HIGH : Severity.MEDIUM;

        String description = buildDescription(atRisk, worst, anyOver);

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title(anyOver ? "Budget at risk: over the limit" : "Budget at risk: nearing the limit")
                .description(description)
                .insightType(InsightType.BUDGET_ALERT)
                .severity(severity)
                .riskType(FinancialRiskType.BUDGET_RISK)
                .category(worst.getCategory())
                .actionSuggestion("Review your " + worst.getCategory()
                        + " spending and rebalance before month-end to stay within budget.")
                .confidence(0.95)
                .build();

        return List.of(draft);
    }

    private String buildDescription(List<BudgetUsageResponse> atRisk, BudgetUsageResponse worst, boolean anyOver) {
        String worstClause = String.format("You have already used %s of your %s budget.",
                MoneyFormatter.percent(worst.getPercentageUsed()), worst.getCategory());
        if (atRisk.size() == 1) {
            return worstClause;
        }
        return String.format("%d budgets are at risk this month. %s", atRisk.size(), worstClause);
    }
}
