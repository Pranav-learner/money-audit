package com.Pranav.finance_tracker.financialintelligence.risk.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.RiskRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.RecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Risk Rule 5 — Subscription Risk.
 *
 * <p>Uses a pluggable {@link RecurringChargeDetector} to identify subscription-like recurring
 * charges (Netflix, Spotify, gym, cloud services, …) <b>purely from spending cadence — no merchant
 * names are hardcoded</b>. Expensive recurring charges (at/above
 * {@link RiskThresholdProperties#getSubscriptionCostThreshold()}) are surfaced with a cancellation
 * recommendation. Because detection lives behind an interface, additional patterns — or a future ML
 * classifier — can be introduced without touching this rule.</p>
 */
@Component
@RequiredArgsConstructor
public class SubscriptionRiskRule implements RiskRule {

    private static final String RULE_KEY = "SUBSCRIPTION_RISK";

    /** Cap the number of subscription alerts per run to avoid overwhelming the user. */
    private static final int MAX_ALERTS = 3;

    private final RecurringChargeDetector recurringChargeDetector;
    private final RiskThresholdProperties thresholds;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public FinancialRiskType riskType() {
        return FinancialRiskType.SUBSCRIPTION_RISK;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<RecurringCharge> charges = recurringChargeDetector.detect(context);
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }

        List<RecurringCharge> expensive = charges.stream()
                .filter(c -> c.getTypicalAmount() != null
                        && c.getTypicalAmount().compareTo(thresholds.getSubscriptionCostThreshold()) >= 0)
                .sorted(Comparator.comparing(RecurringCharge::getTypicalAmount).reversed())
                .limit(MAX_ALERTS)
                .toList();

        List<InsightDraft> drafts = new ArrayList<>();
        for (RecurringCharge charge : expensive) {
            drafts.add(toDraft(charge));
        }
        return drafts;
    }

    private InsightDraft toDraft(RecurringCharge charge) {
        BigDecimal totalPaid = charge.getTypicalAmount().multiply(BigDecimal.valueOf(charge.getOccurrences()));
        String description = String.format(
                "You've paid about %s/month for '%s' across %d months (%s so far). "
                        + "If you no longer use it, cancelling could free up that amount.",
                MoneyFormatter.rupees(charge.getTypicalAmount()), charge.getLabel(),
                charge.getMonthsObserved(), MoneyFormatter.rupees(totalPaid));

        return InsightDraft.builder()
                .ruleKey(RULE_KEY + ":" + charge.getLabel())
                .title("Review your '" + charge.getLabel() + "' subscription")
                .description(description)
                .insightType(InsightType.SAVING_OPPORTUNITY)
                .severity(Severity.MEDIUM)
                .riskType(FinancialRiskType.SUBSCRIPTION_RISK)
                .category(charge.getCategory())
                .actionSuggestion("Cancel or downgrade '" + charge.getLabel()
                        + "' if it's no longer worth " + MoneyFormatter.rupees(charge.getTypicalAmount()) + "/month.")
                .confidence(0.75)
                .build();
    }
}
