package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.Amounts;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rule 1 — Food Spending.
 *
 * <p>When restaurant/food spending rose significantly month-over-month, recommends cutting back a
 * couple of restaurant visits a week and estimates the monthly saving (half of the increase — a
 * realistic, not aggressive, claw-back).</p>
 */
@Component
@RequiredArgsConstructor
public class FoodSpendingRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "FOOD_SPENDING";
    private static final String CATEGORY = "Food";
    private static final BigDecimal SIGNIFICANT_INCREASE = new BigDecimal("0.15");
    private static final BigDecimal RECOVERABLE_FRACTION = new BigDecimal("0.5");

    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.SPENDING;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        InsightContext insight = context.getInsight();
        BigDecimal current = insight.spendForCategory(insight.getCurrentMonthExpenses(), CATEGORY);
        BigDecimal previous = insight.spendForCategory(insight.getPreviousMonthExpenses(), CATEGORY);

        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BigDecimal threshold = previous.add(previous.multiply(SIGNIFICANT_INCREASE));
        if (current.compareTo(threshold) <= 0) {
            return List.of();
        }

        BigDecimal increase = current.subtract(previous);
        BigDecimal saving = Amounts.roundToHundred(increase.multiply(RECOVERABLE_FRACTION));
        if (saving.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        Priority priority = saving.compareTo(new BigDecimal("3000")) >= 0 ? Priority.HIGH : Priority.MEDIUM;
        String description = String.format(
                "You could save approximately %s every month by reducing two restaurant visits each week — "
                        + "your food spending rose %s versus last month.",
                MoneyFormatter.rupees(saving), MoneyFormatter.rupees(increase));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Cut back on dining out")
                .description(description)
                .recommendationType(RecommendationType.SPENDING)
                .priority(priority)
                .expectedMonthlySaving(saving)
                .confidence(0.8)
                .actionText("Plan more meals at home")
                .build());
    }
}
