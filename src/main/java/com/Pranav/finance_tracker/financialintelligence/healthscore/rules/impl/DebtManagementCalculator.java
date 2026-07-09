package com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Scores debt load: debt-free users score full points, degrading with the amount owed and the number
 * of unsettled settlements.
 */
@Component
@RequiredArgsConstructor
public class DebtManagementCalculator implements HealthComponentCalculator {

    /** Amount owed (₹) that, on its own, would roughly halve the debt-health fraction. */
    private static final double AMOUNT_HALF_LIFE = 25000.0;

    /** Per-settlement penalty on the 0–1 debt-health fraction. */
    private static final double PER_SETTLEMENT_PENALTY = 0.12;

    private final HealthScoreProperties properties;

    @Override
    public HealthComponent component() {
        return HealthComponent.DEBT_MANAGEMENT;
    }

    @Override
    public ComponentScore evaluate(InsightContext context) {
        int max = properties.getDebtWeight();
        BigDecimal owed = context.getTotalOwed();
        int settlements = context.getOwedSettlementCount();

        if ((owed == null || owed.compareTo(BigDecimal.ZERO) <= 0) && settlements == 0) {
            return ComponentScore.builder().component(component()).maxPoints(max).score(max)
                    .reason("You're debt-free — excellent.").build();
        }

        double owedAmount = owed == null ? 0 : owed.doubleValue();
        double health = Math.max(0.0, 1.0 - (owedAmount / (2 * AMOUNT_HALF_LIFE)) - settlements * PER_SETTLEMENT_PENALTY);
        int score = (int) Math.round(max * health);
        String reason = String.format("You owe %s across %d settlement(s).",
                MoneyFormatter.rupees(owed == null ? BigDecimal.ZERO : owed), settlements);
        return ComponentScore.builder().component(component()).maxPoints(max).score(score).reason(reason).build();
    }
}
