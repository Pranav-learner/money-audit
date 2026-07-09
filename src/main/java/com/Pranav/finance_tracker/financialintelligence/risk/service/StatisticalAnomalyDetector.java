package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.SpendingAnomaly;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default statistical implementation of {@link SpendingAnomalyDetector}.
 *
 * <p>Two lightweight, explainable signals:</p>
 * <ol>
 *   <li><b>Outlier amount</b> — a current-month expense whose z-score against the trailing-window
 *       distribution meets {@link RiskThresholdProperties#getAnomalyZScore()}.</li>
 *   <li><b>Repeated transactions</b> — the same amount charged to the same category
 *       {@link RiskThresholdProperties#getRepeatedTransactionCount()} times this month.</li>
 * </ol>
 *
 * <p>The z-score maps directly to a confidence and severity, and the whole class hides behind the
 * {@link SpendingAnomalyDetector} interface so it can later be replaced by an ML model with no
 * change to the consuming rule.</p>
 */
@Component
@RequiredArgsConstructor
public class StatisticalAnomalyDetector implements SpendingAnomalyDetector {

    /** Need a reasonable sample before standard deviation is meaningful. */
    private static final int MIN_SAMPLE = 8;

    private final RiskThresholdProperties thresholds;

    @Override
    public List<SpendingAnomaly> detect(InsightContext context) {
        List<Expense> current = context.getCurrentMonthExpenses();
        if (current == null || current.isEmpty()) {
            return List.of();
        }

        List<SpendingAnomaly> anomalies = new ArrayList<>();
        addOutlierAnomaly(context, anomalies);
        addRepeatedTransactionAnomalies(current, anomalies);
        return anomalies;
    }

    private void addOutlierAnomaly(InsightContext context, List<SpendingAnomaly> anomalies) {
        List<BigDecimal> window = context.getWindowExpenses() == null ? List.of()
                : context.getWindowExpenses().stream().map(Expense::getAmount).filter(Objects::nonNull).toList();
        if (window.size() < MIN_SAMPLE) {
            return;
        }

        double mean = window.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = window.stream()
                .mapToDouble(a -> Math.pow(a.doubleValue() - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        if (stdDev <= 0) {
            return;
        }

        // The single most extreme current-month expense above the z-score threshold.
        Expense worst = null;
        double worstZ = thresholds.getAnomalyZScore();
        for (Expense expense : context.getCurrentMonthExpenses()) {
            if (expense.getAmount() == null) {
                continue;
            }
            double z = (expense.getAmount().doubleValue() - mean) / stdDev;
            if (z >= worstZ) {
                worst = expense;
                worstZ = z;
            }
        }
        if (worst == null) {
            return;
        }

        Severity severity = worstZ >= thresholds.getAnomalyHighZScore() ? Severity.HIGH : Severity.MEDIUM;
        double confidence = Math.min(0.95, 0.5 + (worstZ - thresholds.getAnomalyZScore()) * 0.15);
        String label = describe(worst);

        anomalies.add(SpendingAnomaly.builder()
                .label(label)
                .category(categoryName(worst))
                .amount(worst.getAmount())
                .confidence(Math.max(0.5, confidence))
                .suggestedSeverity(severity)
                .explanation(String.format(
                        "%s of %s is well outside your usual spending pattern (%.1f× standard deviations above average).",
                        label, MoneyFormatter.rupees(worst.getAmount()), worstZ))
                .signature("OUTLIER:" + normalise(label))
                .build());
    }

    private void addRepeatedTransactionAnomalies(List<Expense> current, List<SpendingAnomaly> anomalies) {
        Map<String, List<Expense>> byAmountAndCategory = new LinkedHashMap<>();
        for (Expense expense : current) {
            if (expense.getAmount() == null) {
                continue;
            }
            String key = expense.getAmount().stripTrailingZeros().toPlainString() + "|" + categoryName(expense);
            byAmountAndCategory.computeIfAbsent(key, k -> new ArrayList<>()).add(expense);
        }

        for (List<Expense> group : byAmountAndCategory.values()) {
            if (group.size() < thresholds.getRepeatedTransactionCount()) {
                continue;
            }
            Expense sample = group.get(0);
            BigDecimal each = sample.getAmount();
            BigDecimal total = each.multiply(BigDecimal.valueOf(group.size()));
            String label = describe(sample);

            anomalies.add(SpendingAnomaly.builder()
                    .label(label)
                    .category(categoryName(sample))
                    .amount(total)
                    .confidence(0.8)
                    .suggestedSeverity(Severity.MEDIUM)
                    .explanation(String.format(
                            "%d transactions of %s (%s in total) for %s this month — check for accidental repeats.",
                            group.size(), MoneyFormatter.rupees(each), MoneyFormatter.rupees(total), label))
                    .signature("REPEAT:" + normalise(label) + ":" + each.stripTrailingZeros().toPlainString())
                    .build());
        }
    }

    private String describe(Expense expense) {
        if (expense.getTitle() != null && !expense.getTitle().isBlank()) {
            return "'" + expense.getTitle().trim() + "'";
        }
        String category = categoryName(expense);
        return category != null ? "a " + category.toLowerCase() + " expense" : "an expense";
    }

    private String categoryName(Expense expense) {
        return expense.getCategory() != null ? expense.getCategory().getName() : null;
    }

    private String normalise(String label) {
        return label == null ? "" : label.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }
}
