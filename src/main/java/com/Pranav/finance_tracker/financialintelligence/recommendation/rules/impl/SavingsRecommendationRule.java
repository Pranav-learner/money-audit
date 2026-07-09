package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

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
import java.util.List;

/**
 * Rule 5 — Savings.
 *
 * <p>When the user's savings rate (this month's savings as a share of their monthly inflow, proxied
 * by savings + spending) is below the configured target, recommends a concrete additional monthly
 * amount to set aside to reach that target.</p>
 */
@Component
@RequiredArgsConstructor
public class SavingsRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "SAVINGS_RATE";

    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.SAVING;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        BigDecimal saved = context.getSavedThisMonth() == null ? BigDecimal.ZERO : context.getSavedThisMonth();
        BigDecimal spent = context.monthlySpend();
        BigDecimal inflow = saved.add(spent); // income proxy: what flowed through this month

        if (inflow.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        double rate = saved.divide(inflow, 4, RoundingMode.HALF_UP).doubleValue();
        if (rate >= properties.getTargetSavingsRate()) {
            return List.of(); // already saving enough
        }

        BigDecimal targetSetAside = inflow.multiply(BigDecimal.valueOf(properties.getTargetSavingsRate()));
        BigDecimal additional = Amounts.roundToHundred(targetSetAside.subtract(saved));
        if (additional.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        int targetPct = (int) Math.round(properties.getTargetSavingsRate() * 100);
        String description = String.format(
                "You're saving about %d%% of your money this month — aiming for %d%% means setting aside "
                        + "roughly %s more each month.",
                (int) Math.round(rate * 100), targetPct, MoneyFormatter.rupees(additional));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Boost your monthly savings")
                .description(description)
                .recommendationType(RecommendationType.SAVING)
                .priority(Priority.MEDIUM)
                .expectedMonthlySaving(additional)
                .confidence(0.7)
                .actionText("Set aside " + MoneyFormatter.rupees(additional) + " this month")
                .build());
    }
}
