package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.Amounts;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Rule 6 — Budget.
 *
 * <p>When a category budget is exceeded, recommends rebalancing the allocation: either trim
 * spending back toward the limit (the excess is the potential saving) or, if the overshoot is
 * modest and persistent, raise the allocation to a realistic level. Targets the most-overspent
 * category so the advice is specific and actionable.</p>
 */
@Component
public class BudgetRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "BUDGET_ADJUST";
    private static final int OVER_LIMIT_PCT = 100;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.BUDGET;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        List<BudgetUsageResponse> usages = context.getInsight().getBudgetUsages();
        if (usages == null || usages.isEmpty()) {
            return List.of();
        }

        BudgetUsageResponse worst = usages.stream()
                .filter(u -> u.getPercentageUsed() >= OVER_LIMIT_PCT)
                .max(Comparator.comparingInt(BudgetUsageResponse::getPercentageUsed))
                .orElse(null);
        if (worst == null) {
            return List.of();
        }

        BigDecimal overspend = worst.getSpent().subtract(worst.getBudget()).max(BigDecimal.ZERO);
        BigDecimal saving = Amounts.roundToHundred(overspend);

        String description = String.format(
                "You've spent %s against your %s %s budget (%d%% used). Either trim %s spending back "
                        + "toward the limit or raise the allocation to a realistic level.",
                MoneyFormatter.rupees(worst.getSpent()), MoneyFormatter.rupees(worst.getBudget()),
                worst.getCategory(), worst.getPercentageUsed(), worst.getCategory().toLowerCase());

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY + ":" + worst.getCategory())
                .title("Rebalance your " + worst.getCategory() + " budget")
                .description(description)
                .recommendationType(RecommendationType.BUDGET)
                .priority(Priority.MEDIUM)
                .expectedMonthlySaving(saving)
                .confidence(0.8)
                .actionText("Adjust " + worst.getCategory() + " budget")
                .build());
    }
}
