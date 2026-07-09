package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.expense.entity.Expense;
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
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Rule 2 — Shopping.
 *
 * <p>Detects an unnecessary spike in shopping spend (this month well above the user's own recent
 * monthly average) and recommends a realistic monthly spending target — the historical average —
 * with the excess as the potential saving.</p>
 */
@Component
@RequiredArgsConstructor
public class ShoppingRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "SHOPPING_TARGET";
    private static final String CATEGORY = "Shopping";
    private static final BigDecimal SPIKE_MULTIPLIER = new BigDecimal("1.25");

    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.BUDGET;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        InsightContext insight = context.getInsight();
        BigDecimal current = insight.spendForCategory(insight.getCurrentMonthExpenses(), CATEGORY);
        BigDecimal baseline = averagePriorMonthlySpend(insight);
        if (baseline.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        if (current.compareTo(baseline.multiply(SPIKE_MULTIPLIER)) <= 0) {
            return List.of();
        }

        BigDecimal target = Amounts.roundToHundred(baseline);
        BigDecimal saving = Amounts.roundToHundred(current.subtract(baseline));
        if (saving.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        String description = String.format(
                "Your shopping this month (%s) is well above your usual %s. Setting a monthly shopping "
                        + "target of about %s could save you %s.",
                MoneyFormatter.rupees(current), MoneyFormatter.rupees(baseline),
                MoneyFormatter.rupees(target), MoneyFormatter.rupees(saving));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Set a monthly shopping target")
                .description(description)
                .recommendationType(RecommendationType.BUDGET)
                .priority(Priority.MEDIUM)
                .expectedMonthlySaving(saving)
                .confidence(0.75)
                .actionText("Set a " + MoneyFormatter.rupees(target) + " shopping budget")
                .build());
    }

    /** Average monthly shopping spend across prior months present in the trailing window. */
    private BigDecimal averagePriorMonthlySpend(InsightContext insight) {
        List<Expense> window = insight.getWindowExpenses();
        if (window == null || window.isEmpty()) {
            return BigDecimal.ZERO;
        }
        YearMonth currentMonth = insight.getCurrentMonth();
        var byMonth = window.stream()
                .filter(e -> e.getCategory() != null && CATEGORY.equalsIgnoreCase(e.getCategory().getName()))
                .filter(e -> e.getExpenseDate() != null && e.getAmount() != null)
                .filter(e -> !YearMonth.from(e.getExpenseDate()).equals(currentMonth))
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> YearMonth.from(e.getExpenseDate()),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        if (byMonth.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = byMonth.values().stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(byMonth.size()), 2, RoundingMode.HALF_UP);
    }
}
