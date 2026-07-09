package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Risk Rule 2 — Cash Flow Risk.
 *
 * <p>Estimates the monthly spending trend by extrapolating the current run-rate
 * ({@code spendSoFar / daysElapsed × daysInMonth}) and compares the projection against the user's
 * available budget ceiling (the sum of category limits, used here as the income proxy since the
 * domain models no explicit income). When the projection overshoots the ceiling, a HIGH-severity
 * risk is raised quantifying the expected overrun.</p>
 *
 * <p>The projection is only trusted once at least {@link RiskThresholdProperties#getCashflowMinElapsedDays()}
 * days have elapsed, and confidence grows with how far into the month we are.</p>
 */
@Component
@RequiredArgsConstructor
public class CashflowRiskRule implements RiskRule {

    private static final String RULE_KEY = "CASHFLOW_RISK";

    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.CASHFLOW_RISK;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        BigDecimal ceiling = context.totalBudget();
        if (ceiling.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // no budget to project against
        }

        int daysElapsed = context.getToday().getDayOfMonth();
        int daysInMonth = context.getCurrentMonth().lengthOfMonth();
        if (daysElapsed < thresholds.getCashflowMinElapsedDays()) {
            return List.of(); // too early in the month to trust the run-rate
        }

        BigDecimal spentSoFar = context.totalSpend(context.getCurrentMonthExpenses());
        if (spentSoFar.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal projected = spentSoFar
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);

        BigDecimal trigger = ceiling.add(ceiling.multiply(BigDecimal.valueOf(thresholds.getCashflowOverrunBuffer())));
        if (projected.compareTo(trigger) <= 0) {
            return List.of();
        }

        BigDecimal overrun = projected.subtract(ceiling);
        double progress = (double) daysElapsed / daysInMonth;
        double confidence = Math.min(0.95, 0.6 + progress * 0.3);

        String description = String.format(
                "At your current spending rate you may exceed your monthly budget by %s "
                        + "(projected %s vs a %s budget).",
                MoneyFormatter.rupees(overrun), MoneyFormatter.rupees(projected), MoneyFormatter.rupees(ceiling));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Cash-flow risk: on track to overspend")
                .description(description)
                .insightType(InsightType.BUDGET_ALERT)
                .severity(Severity.HIGH)
                .riskType(FinancialRiskType.CASHFLOW_RISK)
                .category(null)
                .actionSuggestion("Trim discretionary spending over the remaining "
                        + (daysInMonth - daysElapsed) + " day(s) to close the projected gap.")
                .confidence(confidence)
                .build();

        return List.of(draft);
    }
}
