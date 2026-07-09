package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.analytics.dto.SavingTrendItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Risk Rule 4 — Savings Risk.
 *
 * <p>Detects a <b>declining savings trend</b>: it compares the most recently completed month's
 * savings against the average of the preceding months and raises a reminder when the drop exceeds
 * {@link RiskThresholdProperties#getSavingsDeclinePercent()}. Stopping saving entirely (after a
 * healthy baseline) escalates to HIGH severity.</p>
 *
 * <p>Plain savings <i>inactivity</i> is already covered by the Module&nbsp;1
 * {@code NoSavingsActivityRule}; this rule deliberately focuses on the trend so the two are
 * complementary rather than duplicative.</p>
 */
@Component
@RequiredArgsConstructor
public class SavingsRiskRule implements RiskRule {

    private static final String RULE_KEY = "SAVINGS_RISK";

    /** Number of prior months averaged to form the baseline. */
    private static final int BASELINE_MONTHS = 3;

    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.SAVINGS_RISK;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<SavingTrendItem> trend = context.getSavingsTrend();
        if (trend == null || trend.isEmpty()) {
            return List.of();
        }

        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        for (SavingTrendItem item : trend) {
            byMonth.put(item.getMonth(), item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
        }

        int lastCompleted = context.getCurrentMonth().getMonthValue() - 1;
        if (lastCompleted < BASELINE_MONTHS + 1) {
            return List.of(); // not enough completed months this year to judge a trend
        }

        BigDecimal recent = byMonth.getOrDefault(lastCompleted, BigDecimal.ZERO);

        BigDecimal baselineSum = BigDecimal.ZERO;
        for (int m = lastCompleted - BASELINE_MONTHS; m < lastCompleted; m++) {
            baselineSum = baselineSum.add(byMonth.getOrDefault(m, BigDecimal.ZERO));
        }
        BigDecimal baselineAvg = baselineSum.divide(BigDecimal.valueOf(BASELINE_MONTHS), 2, RoundingMode.HALF_UP);
        if (baselineAvg.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // no baseline saving habit to decline from
        }

        BigDecimal declineTrigger = baselineAvg
                .multiply(BigDecimal.valueOf(100 - thresholds.getSavingsDeclinePercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (recent.compareTo(declineTrigger) >= 0) {
            return List.of();
        }

        int dropPct = baselineAvg.subtract(recent)
                .multiply(BigDecimal.valueOf(100))
                .divide(baselineAvg, 0, RoundingMode.HALF_UP)
                .intValue();
        boolean stopped = recent.compareTo(BigDecimal.ZERO) <= 0;
        Severity severity = stopped ? Severity.HIGH : Severity.MEDIUM;

        String description = stopped
                ? String.format("You saved nothing last month, down from a recent average of %s.",
                        MoneyFormatter.rupees(baselineAvg))
                : String.format("Your savings fell to %s last month — about %s below your recent average of %s.",
                        MoneyFormatter.rupees(recent), MoneyFormatter.percent(dropPct), MoneyFormatter.rupees(baselineAvg));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Your savings are trending down")
                .description(description)
                .insightType(InsightType.INFORMATION)
                .severity(severity)
                .riskType(FinancialRiskType.SAVINGS_RISK)
                .category(null)
                .actionSuggestion("Aim to set aside at least " + MoneyFormatter.rupees(baselineAvg)
                        + " this month to get your savings back on track.")
                .confidence(0.85)
                .build();

        return List.of(draft);
    }
}
