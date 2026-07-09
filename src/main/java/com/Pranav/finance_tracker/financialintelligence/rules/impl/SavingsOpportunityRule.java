package com.Pranav.finance_tracker.financialintelligence.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightRule;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Rule 5 — turns the category with the largest month-over-month growth into a savings target.
 *
 * <p>Finds the category whose spending rose the most versus last month and recommends a
 * realistic monthly saving (half of the increase, rounded to the nearest ₹100).</p>
 */
@Component
public class SavingsOpportunityRule implements InsightRule {

    private static final String RULE_KEY = "SAVINGS_OPPORTUNITY";

    /** Ignore trivial increases below this absolute amount. */
    private static final BigDecimal MIN_INCREASE = new BigDecimal("500");

    /** Fraction of the increase we suggest the user could realistically claw back. */
    private static final BigDecimal RECOVERABLE_FRACTION = new BigDecimal("0.5");

    private static final BigDecimal ROUNDING_STEP = new BigDecimal("100");

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        Map<String, BigDecimal> current = context.categoryTotals(context.getCurrentMonthExpenses());
        Map<String, BigDecimal> previous = context.categoryTotals(context.getPreviousMonthExpenses());

        String topCategory = null;
        BigDecimal topIncrease = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : current.entrySet()) {
            BigDecimal prev = previous.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal increase = entry.getValue().subtract(prev);
            if (increase.compareTo(topIncrease) > 0) {
                topIncrease = increase;
                topCategory = entry.getKey();
            }
        }

        if (topCategory == null || topIncrease.compareTo(MIN_INCREASE) < 0) {
            return List.of();
        }

        BigDecimal potentialSaving = roundToStep(topIncrease.multiply(RECOVERABLE_FRACTION));
        if (potentialSaving.compareTo(ROUNDING_STEP) < 0) {
            return List.of();
        }

        String description = String.format(
                "You could save approximately %s/month by reducing %s spending, which rose %s versus last month.",
                MoneyFormatter.rupees(potentialSaving),
                topCategory.toLowerCase(),
                MoneyFormatter.rupees(topIncrease));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Savings opportunity in " + topCategory)
                .description(description)
                .insightType(InsightType.SAVING_OPPORTUNITY)
                .severity(Severity.LOW)
                .category(topCategory)
                .actionSuggestion("Try trimming " + topCategory.toLowerCase()
                        + " spending back toward last month's level and move the difference into savings.")
                .confidence(0.7)
                .build();

        return List.of(draft);
    }

    private BigDecimal roundToStep(BigDecimal value) {
        return value.divide(ROUNDING_STEP, 0, RoundingMode.HALF_UP).multiply(ROUNDING_STEP);
    }
}
