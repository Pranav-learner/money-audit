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

/**
 * Rule 1 — flags a significant month-over-month increase in Food spending.
 *
 * <p>Triggers when this month's food spend exceeds last month's by more than 15%.
 * The message is personalized with the actual amount and percentage.</p>
 */
@Component
public class FoodSpendingIncreasedRule implements InsightRule {

    private static final String RULE_KEY = "FOOD_SPENDING_INCREASED";
    private static final String CATEGORY = "Food";

    /** Minimum relative increase (15%) before we consider it worth flagging. */
    private static final BigDecimal SIGNIFICANT_INCREASE = new BigDecimal("0.15");

    /** Above this increase the warning is escalated to HIGH severity. */
    private static final int HIGH_SEVERITY_PCT = 40;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        BigDecimal current = context.spendForCategory(context.getCurrentMonthExpenses(), CATEGORY);
        BigDecimal previous = context.spendForCategory(context.getPreviousMonthExpenses(), CATEGORY);

        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // no baseline to compare against
        }

        BigDecimal threshold = previous.add(previous.multiply(SIGNIFICANT_INCREASE));
        if (current.compareTo(threshold) <= 0) {
            return List.of();
        }

        int increasePct = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP)
                .intValue();

        Severity severity = increasePct >= HIGH_SEVERITY_PCT ? Severity.HIGH : Severity.MEDIUM;

        String description = String.format(
                "You spent %s on food this month, which is %s higher than last month.",
                MoneyFormatter.rupees(current), MoneyFormatter.percent(increasePct));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Food spending increased")
                .description(description)
                .insightType(InsightType.SPENDING_WARNING)
                .severity(severity)
                .category(CATEGORY)
                .actionSuggestion("Reducing restaurant visits by two per week could help lower expenses.")
                .confidence(0.85)
                .build();

        return List.of(draft);
    }
}
