package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rule 3 — Subscription.
 *
 * <p>Reuses the risk module's {@link RecurringChargeDetector} to find expensive recurring
 * subscriptions and recommends cancelling or downgrading each, quantifying the annual saving.
 * No merchant names are hardcoded — the detector infers subscriptions from spending cadence.</p>
 */
@Component
@RequiredArgsConstructor
public class SubscriptionRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "SUBSCRIPTION_CANCEL";
    private static final int MAX_RECOMMENDATIONS = 3;

    private final RecurringChargeDetector recurringChargeDetector;
    private final RecommendationProperties properties;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.SUBSCRIPTION;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        List<RecurringCharge> charges = recurringChargeDetector.detect(context.getInsight());
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }

        List<RecurringCharge> candidates = charges.stream()
                .filter(c -> c.getTypicalAmount() != null
                        && c.getTypicalAmount().compareTo(properties.getMinSavingsThreshold()) >= 0)
                .sorted(Comparator.comparing(RecurringCharge::getTypicalAmount).reversed())
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        List<RecommendationDraft> drafts = new ArrayList<>();
        for (RecurringCharge charge : candidates) {
            BigDecimal monthly = charge.getTypicalAmount();
            BigDecimal annual = monthly.multiply(BigDecimal.valueOf(12));
            Priority priority = monthly.compareTo(new BigDecimal("800")) >= 0 ? Priority.HIGH : Priority.MEDIUM;

            drafts.add(RecommendationDraft.builder()
                    .ruleKey(RULE_KEY + ":" + charge.getLabel())
                    .title("Cancel or downgrade '" + charge.getLabel() + "'")
                    .description(String.format(
                            "'%s' costs about %s/month — roughly %s a year. If you rarely use it, "
                                    + "cancelling or moving to a cheaper plan frees up that money.",
                            charge.getLabel(), MoneyFormatter.rupees(monthly), MoneyFormatter.rupees(annual)))
                    .recommendationType(RecommendationType.SUBSCRIPTION)
                    .priority(priority)
                    .expectedMonthlySaving(monthly)
                    .confidence(0.75)
                    .actionText("Review '" + charge.getLabel() + "'")
                    .build());
        }
        return drafts;
    }
}
