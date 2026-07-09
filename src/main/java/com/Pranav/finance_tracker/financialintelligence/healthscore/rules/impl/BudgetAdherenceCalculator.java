package com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scores how well the user stays within their category budgets: the lower the average usage, the
 * more points awarded.
 */
@Component
@RequiredArgsConstructor
public class BudgetAdherenceCalculator implements HealthComponentCalculator {

    private final HealthScoreProperties properties;

    @Override
    public HealthComponent component() {
        return HealthComponent.BUDGET_ADHERENCE;
    }

    @Override
    public ComponentScore evaluate(InsightContext context) {
        int max = properties.getBudgetWeight();
        List<BudgetUsageResponse> usages = context.getBudgetUsages();
        if (usages == null || usages.isEmpty()) {
            return ComponentScore.builder()
                    .component(component()).maxPoints(max).score((int) Math.round(max * 0.7))
                    .reason("No budgets set — assuming neutral budget health. Set budgets for a sharper score.")
                    .build();
        }

        double avgUsage = usages.stream().mapToInt(BudgetUsageResponse::getPercentageUsed).average().orElse(0);
        double retained = Math.max(0, 1.0 - avgUsage / 100.0);
        int score = (int) Math.round(max * retained);

        String reason = avgUsage <= 80
                ? String.format("Budgets are healthy (average usage %.0f%%).", avgUsage)
                : String.format("Budgets are stretched (average usage %.0f%%).", avgUsage);
        return ComponentScore.builder().component(component()).maxPoints(max).score(score).reason(reason).build();
    }
}
