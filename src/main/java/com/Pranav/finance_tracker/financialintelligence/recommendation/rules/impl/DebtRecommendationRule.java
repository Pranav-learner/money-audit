package com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.RecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rule 4 — Debt.
 *
 * <p>When outstanding debt is high, recommends a settlement-priority strategy (clear the largest
 * balances first). Debt reduction is not a recurring "saving", so no monthly-saving figure is
 * attached; the recommendation is ranked by urgency instead.</p>
 */
@Component
public class DebtRecommendationRule implements RecommendationRule {

    private static final String RULE_KEY = "DEBT_SETTLEMENT";

    /** Outstanding amount (₹) above which debt advice is worthwhile. */
    private static final BigDecimal DEBT_FLOOR = new BigDecimal("5000");

    /** Number of open settlements that on its own justifies advice. */
    private static final int SETTLEMENT_FLOOR = 3;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public RecommendationType type() {
        return RecommendationType.DEBT;
    }

    @Override
    public List<RecommendationDraft> evaluate(RecommendationContext context) {
        InsightContext insight = context.getInsight();
        BigDecimal owed = insight.getTotalOwed();
        if (owed == null || owed.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        int settlements = insight.getOwedSettlementCount();
        if (owed.compareTo(DEBT_FLOOR) < 0 && settlements < SETTLEMENT_FLOOR) {
            return List.of();
        }

        boolean severe = owed.compareTo(DEBT_FLOOR.multiply(BigDecimal.valueOf(2))) >= 0
                || settlements >= SETTLEMENT_FLOOR * 2;
        Priority priority = severe ? Priority.CRITICAL : Priority.HIGH;

        String settlementClause = settlements > 0
                ? String.format(" across %d settlement%s", settlements, settlements == 1 ? "" : "s")
                : "";
        String description = String.format(
                "You owe %s%s. Clear the largest balance first, then work down the rest — this reduces "
                        + "what you owe fastest and avoids balances piling up.",
                MoneyFormatter.rupees(owed), settlementClause);

        return List.of(RecommendationDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Prioritise settling your debts")
                .description(description)
                .recommendationType(RecommendationType.DEBT)
                .priority(priority)
                .expectedMonthlySaving(BigDecimal.ZERO)
                .confidence(0.9)
                .actionText("Settle your largest balance")
                .build());
    }
}
