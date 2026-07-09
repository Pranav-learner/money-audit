package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default, heuristic implementation of {@link RecurringChargeDetector}.
 *
 * <p>Groups the trailing-window expenses by a normalised label (expense title, falling back to
 * category) and treats a group as recurring when it appears in at least
 * {@link RiskThresholdProperties#getSubscriptionMinMonths()} distinct months. The representative
 * amount and day-of-month are the medians of the group, which are robust to the occasional
 * irregular charge. No merchant names are hardcoded — recurrence is inferred purely from cadence,
 * so new "subscriptions" are picked up automatically and the whole strategy can be swapped for an
 * ML detector without touching the rules that consume {@link RecurringCharge}s.</p>
 */
@Component
@RequiredArgsConstructor
public class HeuristicRecurringChargeDetector implements RecurringChargeDetector {

    private final RiskThresholdProperties thresholds;

    @Override
    public List<RecurringCharge> detect(InsightContext context) {
        List<Expense> expenses = context.getWindowExpenses();
        if (expenses == null || expenses.isEmpty()) {
            return List.of();
        }

        // Preserve first-seen order for deterministic output.
        Map<String, List<Expense>> byLabel = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            if (expense.getAmount() == null || expense.getExpenseDate() == null) {
                continue;
            }
            String label = normalisedLabel(expense);
            if (label == null) {
                continue;
            }
            byLabel.computeIfAbsent(label, k -> new ArrayList<>()).add(expense);
        }

        List<RecurringCharge> charges = new ArrayList<>();
        for (Map.Entry<String, List<Expense>> entry : byLabel.entrySet()) {
            List<Expense> group = entry.getValue();
            long distinctMonths = group.stream()
                    .map(e -> YearMonth.from(e.getExpenseDate()))
                    .distinct()
                    .count();
            if (distinctMonths < thresholds.getSubscriptionMinMonths()) {
                continue;
            }

            charges.add(RecurringCharge.builder()
                    .label(displayLabel(group.get(0)))
                    .category(categoryName(group.get(0)))
                    .typicalAmount(median(group.stream().map(Expense::getAmount).toList()))
                    .occurrences(group.size())
                    .monthsObserved((int) distinctMonths)
                    .typicalDayOfMonth(medianDayOfMonth(group))
                    .lastSeen(group.stream().map(Expense::getExpenseDate).max(Comparator.naturalOrder()).orElse(null))
                    .build());
        }
        return charges;
    }

    private String normalisedLabel(Expense expense) {
        if (expense.getTitle() != null && !expense.getTitle().isBlank()) {
            return expense.getTitle().trim().toLowerCase();
        }
        String category = categoryName(expense);
        return category != null ? "category:" + category.toLowerCase() : null;
    }

    private String displayLabel(Expense expense) {
        if (expense.getTitle() != null && !expense.getTitle().isBlank()) {
            return expense.getTitle().trim();
        }
        return categoryName(expense);
    }

    private String categoryName(Expense expense) {
        return expense.getCategory() != null ? expense.getCategory().getName() : null;
    }

    private BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().filter(Objects::nonNull).sorted().toList();
        if (sorted.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return sorted.get(mid - 1).add(sorted.get(mid))
                .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
    }

    private Integer medianDayOfMonth(List<Expense> group) {
        List<Integer> days = group.stream()
                .map(e -> e.getExpenseDate().getDayOfMonth())
                .sorted()
                .toList();
        if (days.isEmpty()) {
            return null;
        }
        return days.get(days.size() / 2);
    }
}
