package com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Scores savings behaviour by recency of the last contribution: recent savers score full points,
 * decaying to zero after a long gap.
 */
@Component
@RequiredArgsConstructor
public class SavingsBehaviorCalculator implements HealthComponentCalculator {

    private static final long FULL_POINTS_WITHIN_DAYS = 30;
    private static final long ZERO_POINTS_AFTER_DAYS = 120;

    private final HealthScoreProperties properties;

    @Override
    public HealthComponent component() {
        return HealthComponent.SAVINGS_BEHAVIOR;
    }

    @Override
    public ComponentScore evaluate(InsightContext context) {
        int max = properties.getSavingsWeight();
        long days = context.daysSinceLastSaving();

        int score;
        String reason;
        if (days <= FULL_POINTS_WITHIN_DAYS) {
            score = max;
            reason = "You've saved recently — great savings habit.";
        } else if (days >= ZERO_POINTS_AFTER_DAYS) {
            score = 0;
            reason = String.format("No savings recorded in %d days.", days);
        } else {
            double retained = 1.0 - (days - FULL_POINTS_WITHIN_DAYS) / (double) (ZERO_POINTS_AFTER_DAYS - FULL_POINTS_WITHIN_DAYS);
            score = (int) Math.round(max * retained);
            reason = String.format("It's been %d days since your last savings contribution.", days);
        }
        return ComponentScore.builder().component(component()).maxPoints(max).score(score).reason(reason).build();
    }
}
