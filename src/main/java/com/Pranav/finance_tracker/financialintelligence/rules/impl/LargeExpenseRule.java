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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rule 3 — detects an unusually large expense in the current month.
 *
 * <p>Compares each of this month's expenses against the user's historical average expense
 * amount (over the trailing window). The single largest expense that is at least
 * {@value #UNUSUAL_MULTIPLIER}× the average (and above a sensible floor) is flagged.</p>
 */
@Component
public class LargeExpenseRule implements InsightRule {

    private static final String RULE_KEY = "LARGE_EXPENSE";

    /** Need at least this many historical expenses before an average is trustworthy. */
    private static final int MIN_SAMPLE = 5;

    /** A single expense this many times the average is considered "unusually large". */
    private static final BigDecimal UNUSUAL_MULTIPLIER = new BigDecimal("3");

    /** Ignore small absolute amounts even if they beat the multiplier. */
    private static final BigDecimal ABSOLUTE_FLOOR = new BigDecimal("500");

    /** At/above this multiple the warning is escalated to HIGH severity. */
    private static final BigDecimal HIGH_SEVERITY_MULTIPLIER = new BigDecimal("5");

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        Optional<BigDecimal> maybeAverage = context.historicalAverageExpenseAmount(MIN_SAMPLE);
        if (maybeAverage.isEmpty()) {
            return List.of();
        }
        BigDecimal average = maybeAverage.get();
        if (average.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal unusualThreshold = average.multiply(UNUSUAL_MULTIPLIER).max(ABSOLUTE_FLOOR);

        Optional<Expense> largest = context.getCurrentMonthExpenses().stream()
                .filter(e -> e.getAmount() != null)
                .filter(e -> e.getAmount().compareTo(unusualThreshold) >= 0)
                .max(Comparator.comparing(Expense::getAmount));

        if (largest.isEmpty()) {
            return List.of();
        }

        Expense expense = largest.get();
        BigDecimal ratio = expense.getAmount().divide(average, 1, RoundingMode.HALF_UP);
        Severity severity = ratio.compareTo(HIGH_SEVERITY_MULTIPLIER) >= 0 ? Severity.HIGH : Severity.MEDIUM;

        String label = describe(expense);
        String description = String.format(
                "You spent %s on %s — about %s× your typical expense of %s.",
                MoneyFormatter.rupees(expense.getAmount()),
                label,
                ratio.stripTrailingZeros().toPlainString(),
                MoneyFormatter.rupees(average));

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Unusually large expense detected")
                .description(description)
                .insightType(InsightType.SPENDING_WARNING)
                .severity(severity)
                .category(expense.getCategory() != null ? expense.getCategory().getName() : null)
                .actionSuggestion("If this was one-off, no action needed — otherwise, plan ahead so large costs don't strain your month.")
                .confidence(0.8)
                .build();

        return List.of(draft);
    }

    private String describe(Expense expense) {
        if (expense.getTitle() != null && !expense.getTitle().isBlank()) {
            return "'" + expense.getTitle().trim() + "'";
        }
        if (expense.getCategory() != null && expense.getCategory().getName() != null) {
            return "a " + expense.getCategory().getName().toLowerCase() + " expense";
        }
        return "an expense";
    }
}
