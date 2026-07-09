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
import java.util.List;

/**
 * Rule 9 — Emergency Fund.
 *
 * <p>Estimates a target emergency fund (a configurable number of months of spending), compares it
 * against the user's total savings and — if there is a shortfall — recommends a monthly contribution
 * sized to close the gap over a year. Priority rises the thinner the user's current cushion.</p>
 */
@Component
@RequiredArgsConstructor
public class EmergencyFundRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "EMERGENCY_FUND";
    private static final BigDecimal MONTHS_TO_CLOSE_GAP = new BigDecimal("12");

    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.GOAL;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        BigDecimal monthlySpend = context.monthlySpend();
        if (monthlySpend.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // no spending baseline to size a fund against
        }

        BigDecimal target = monthlySpend.multiply(BigDecimal.valueOf(properties.getEmergencyFundMonths()));
        BigDecimal current = context.getTotalSavings() == null ? BigDecimal.ZERO : context.getTotalSavings();
        BigDecimal gap = target.subtract(current);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(); // already funded
        }

        BigDecimal contribution = Amounts.roundToHundred(gap.divide(MONTHS_TO_CLOSE_GAP, 2, java.math.RoundingMode.HALF_UP));
        if (contribution.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        // Thinner cushions are more urgent: below one month of spending is HIGH.
        Priority priority = current.compareTo(monthlySpend) < 0 ? Priority.HIGH : Priority.MEDIUM;

        String description = String.format(
                "A safety net of %d months of spending is about %s, but you have %s saved. Setting aside "
                        + "roughly %s a month would build that cushion within a year.",
                properties.getEmergencyFundMonths(), MoneyFormatter.rupees(target),
                MoneyFormatter.rupees(current), MoneyFormatter.rupees(contribution));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Build an emergency fund")
                .description(description)
                .recommendationType(RecommendationType.GOAL)
                .priority(priority)
                .expectedMonthlySaving(contribution)
                .confidence(0.7)
                .actionText("Start an emergency fund")
                .build());
    }
}
