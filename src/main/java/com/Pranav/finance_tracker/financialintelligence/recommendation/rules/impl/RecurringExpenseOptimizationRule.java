package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.Amounts;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rule 8 — Recurring Expense Optimization.
 *
 * <p>Looks at the user's total recurring monthly spend (internet, streaming, gym, insurance, cloud
 * services, …) detected by the {@link RecurringChargeDetector} and recommends optimising it —
 * renegotiating, switching providers or bundling — estimating a modest, achievable saving as a
 * share of the total. Complements the subscription rule (which cancels individual items) by taking
 * a portfolio view of ongoing commitments.</p>
 */
@Component
@RequiredArgsConstructor
public class RecurringExpenseOptimizationRule implements RecommendationRule {

    private static final String RULE_KEY = "RECURRING_OPTIMIZATION";

    /** Realistic share of recurring spend recoverable through optimisation. */
    private static final BigDecimal OPTIMISATION_FRACTION = new BigDecimal("0.15");

    /** Need at least a couple of recurring commitments before portfolio advice makes sense. */
    private static final int MIN_RECURRING_CHARGES = 2;

    private final RecurringChargeDetector recurringChargeDetector;
    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.SPENDING;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        List<RecurringCharge> charges = recurringChargeDetector.detect(context.getInsight());
        if (charges == null || charges.size() < MIN_RECURRING_CHARGES) {
            return List.of();
        }

        BigDecimal totalMonthly = charges.stream()
                .map(RecurringCharge::getTypicalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalMonthly.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal saving = Amounts.roundToHundred(totalMonthly.multiply(OPTIMISATION_FRACTION));
        if (saving.compareTo(properties.getMinSavingsThreshold()) < 0) {
            return List.of();
        }

        String description = String.format(
                "You have %d recurring commitments totalling about %s/month. Renegotiating, switching "
                        + "providers or bundling could realistically save around %s a month.",
                charges.size(), MoneyFormatter.rupees(totalMonthly), MoneyFormatter.rupees(saving));

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Optimise your recurring bills")
                .description(description)
                .recommendationType(RecommendationType.SPENDING)
                .priority(Priority.MEDIUM)
                .expectedMonthlySaving(saving)
                .confidence(0.65)
                .actionText("Review recurring bills")
                .build());
    }
}
