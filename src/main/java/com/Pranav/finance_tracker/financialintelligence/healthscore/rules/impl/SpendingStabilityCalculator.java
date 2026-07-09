package com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scores how stable month-to-month spending is. Volatile spending (a high coefficient of variation
 * across completed months) reduces the score; steady spending scores well.
 */
@Component
@RequiredArgsConstructor
public class SpendingStabilityCalculator implements HealthComponentCalculator {

    private static final int MIN_MONTHS = 2;

    private final HealthScoreProperties properties;

    @Override
    public HealthComponent component() {
        return HealthComponent.SPENDING_STABILITY;
    }

    @Override
    public ComponentScore evaluate(InsightContext context) {
        int max = properties.getSpendingWeight();
        List<Expense> window = context.getWindowExpenses();
        YearMonth currentMonth = context.getCurrentMonth();

        if (window == null || window.isEmpty()) {
            return neutral(max, "Not enough history to assess spending stability.");
        }

        // Totals per completed month (exclude the current, partial month).
        Map<YearMonth, BigDecimal> monthly = window.stream()
                .filter(e -> e.getExpenseDate() != null && e.getAmount() != null)
                .filter(e -> !YearMonth.from(e.getExpenseDate()).equals(currentMonth))
                .collect(Collectors.groupingBy(
                        e -> YearMonth.from(e.getExpenseDate()),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        if (monthly.size() < MIN_MONTHS) {
            return neutral(max, "Not enough months of history to assess spending stability.");
        }

        double mean = monthly.values().stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (mean <= 0) {
            return neutral(max, "Not enough spending history to assess stability.");
        }
        double variance = monthly.values().stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2)).average().orElse(0);
        double cv = Math.sqrt(variance) / mean; // coefficient of variation
        double stability = Math.max(0.0, Math.min(1.0, 1.0 - cv));
        int score = (int) Math.round(max * stability);

        String reason = cv <= 0.25
                ? "Your month-to-month spending is steady."
                : "Your spending varies noticeably from month to month.";
        return ComponentScore.builder().component(component()).maxPoints(max).score(score).reason(reason).build();
    }

    private ComponentScore neutral(int max, String reason) {
        return ComponentScore.builder()
                .component(component()).maxPoints(max).score((int) Math.round(max * 0.6)).reason(reason).build();
    }
}
