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
import java.time.LocalDate;
import java.util.List;

/**
 * Risk Rule 6 — Expense Spike.
 *
 * <p>Compares spend over the trailing 7 days (the "current week") against the average weekly spend
 * of the four preceding weeks. When the current week exceeds that baseline by more than
 * {@link RiskThresholdProperties#getSpikeMultiplier()}× (and clears a noise floor), an insight is
 * raised; a particularly large spike escalates to HIGH severity.</p>
 */
@Component
@RequiredArgsConstructor
public class SpendingSpikeRule implements RiskRule {

    private static final String RULE_KEY = "SPENDING_SPIKE";

    /** Ratio at/above which the spike is treated as HIGH severity. */
    private static final BigDecimal HIGH_SEVERITY_RATIO = new BigDecimal("2.5");

    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.SPENDING_SPIKE;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        LocalDate today = context.getToday();

        BigDecimal currentWeek = context.spendBetween(today.minusDays(6), today);
        if (currentWeek.compareTo(thresholds.getSpikeMinWeeklyAmount()) < 0) {
            return List.of();
        }

        // Four weeks immediately preceding the current one.
        BigDecimal priorFourWeeks = context.spendBetween(today.minusDays(34), today.minusDays(7));
        BigDecimal avgWeekly = priorFourWeeks.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        if (avgWeekly.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal trigger = avgWeekly.multiply(BigDecimal.valueOf(thresholds.getSpikeMultiplier()));
        if (currentWeek.compareTo(trigger) <= 0) {
            return List.of();
        }

        BigDecimal ratio = currentWeek.divide(avgWeekly, 1, RoundingMode.HALF_UP);
        Severity severity = ratio.compareTo(HIGH_SEVERITY_RATIO) >= 0 ? Severity.HIGH : Severity.MEDIUM;

        String description = String.format(
                "You've spent %s this week — about %s× your recent weekly average of %s.",
                MoneyFormatter.rupees(currentWeek), ratio.stripTrailingZeros().toPlainString(),
                MoneyFormatter.rupees(avgWeekly));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Spending spike this week")
                .description(description)
                .insightType(InsightType.SPENDING_WARNING)
                .severity(severity)
                .riskType(FinancialRiskType.SPENDING_SPIKE)
                .category(null)
                .actionSuggestion("Check what drove this week's spending and ease off for the next few days.")
                .confidence(0.8)
                .build();

        return List.of(draft);
    }
}
