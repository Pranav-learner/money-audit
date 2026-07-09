package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.Amounts;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rule 7 — Weekend Spending.
 *
 * <p>When average daily spend on weekends materially exceeds weekdays, recommends a realistic
 * reduction and estimates the monthly impact (recovering part of the weekend premium across the
 * ~8 weekend days in a month).</p>
 */
@Component
@RequiredArgsConstructor
public class WeekendSpendingRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "WEEKEND_SPENDING";
    private static final int MIN_DAYS_PER_GROUP = 3;
    private static final BigDecimal SIGNIFICANCE = new BigDecimal("1.20");
    private static final BigDecimal RECOVERABLE_FRACTION = new BigDecimal("0.3");
    private static final BigDecimal WEEKEND_DAYS_PER_MONTH = new BigDecimal("8");

    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.HABIT;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        List<Expense> expenses = context.getInsight().getWindowExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, BigDecimal> dailyTotals = expenses.stream()
                .filter(e -> e.getExpenseDate() != null && e.getAmount() != null)
                .collect(Collectors.groupingBy(Expense::getExpenseDate,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        BigDecimal weekendSum = BigDecimal.ZERO;
        BigDecimal weekdaySum = BigDecimal.ZERO;
        int weekendDays = 0;
        int weekdayDays = 0;
        for (var entry : dailyTotals.entrySet()) {
            if (isWeekend(entry.getKey())) {
                weekendSum = weekendSum.add(entry.getValue());
                weekendDays++;
            } else {
                weekdaySum = weekdaySum.add(entry.getValue());
                weekdayDays++;
            }
        }
        if (weekendDays < MIN_DAYS_PER_GROUP || weekdayDays < MIN_DAYS_PER_GROUP) {
            return List.of();
        }

        BigDecimal weekendAvg = weekendSum.divide(BigDecimal.valueOf(weekendDays), 2, RoundingMode.HALF_UP);
        BigDecimal weekdayAvg = weekdaySum.divide(BigDecimal.valueOf(weekdayDays), 2, RoundingMode.HALF_UP);
        if (weekdayAvg.compareTo(BigDecimal.ZERO) <= 0
                || weekendAvg.compareTo(weekdayAvg.multiply(SIGNIFICANCE)) <= 0) {
            return List.of();
        }

        BigDecimal premiumPerDay = weekendAvg.subtract(weekdayAvg);
        BigDecimal saving = Amounts.roundToHundred(
                premiumPerDay.multiply(RECOVERABLE_FRACTION).multiply(WEEKEND_DAYS_PER_MONTH));
        if (saving.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        String description = String.format(
                "You spend %s per day on weekends versus %s on weekdays. Reining that in a little could "
                        + "save you around %s a month.",
                MoneyFormatter.rupees(weekendAvg), MoneyFormatter.rupees(weekdayAvg), MoneyFormatter.rupees(saving));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Tame weekend spending")
                .description(description)
                .recommendationType(RecommendationType.HABIT)
                .priority(Priority.LOW)
                .expectedMonthlySaving(saving)
                .confidence(0.7)
                .actionText("Set a weekend budget")
                .build());
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
