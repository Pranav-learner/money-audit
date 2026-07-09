package com.Pranav.finance_tracker.financialintelligence.rules.impl;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightRule;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Rule 4 — highlights when average daily spend on weekends consistently exceeds weekdays.
 *
 * <p>Groups the trailing window's expenses by day, classifies each active day as weekend or
 * weekday, then compares the mean daily spend of the two groups. Requires a minimum number of
 * active days in each group so a single splurge does not trigger a false positive.</p>
 */
@Component
public class WeekendSpendingRule implements InsightRule {

    private static final String RULE_KEY = "WEEKEND_SPENDING";
    private static final int MIN_DAYS_PER_GROUP = 3;

    /** Weekend average must exceed weekday average by at least 20% to be "consistent". */
    private static final BigDecimal SIGNIFICANCE = new BigDecimal("1.20");

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<Expense> expenses = context.getWindowExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, BigDecimal> dailyTotals = expenses.stream()
                .filter(e -> e.getExpenseDate() != null && e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Expense::getExpenseDate,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        BigDecimal weekendSum = BigDecimal.ZERO;
        BigDecimal weekdaySum = BigDecimal.ZERO;
        int weekendDays = 0;
        int weekdayDays = 0;

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyTotals.entrySet()) {
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

        int higherPct = weekendAvg.subtract(weekdayAvg)
                .multiply(BigDecimal.valueOf(100))
                .divide(weekdayAvg, 0, RoundingMode.HALF_UP)
                .intValue();

        String description = String.format(
                "You spend %s per day on weekends — %s more than your %s weekday average.",
                MoneyFormatter.rupees(weekendAvg),
                MoneyFormatter.percent(higherPct),
                MoneyFormatter.rupees(weekdayAvg));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Weekends are your biggest spending days")
                .description(description)
                .insightType(InsightType.TREND)
                .severity(Severity.LOW)
                .category(null)
                .actionSuggestion("Setting a small weekend budget could smooth out your spending.")
                .confidence(0.75)
                .build();

        return List.of(draft);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
