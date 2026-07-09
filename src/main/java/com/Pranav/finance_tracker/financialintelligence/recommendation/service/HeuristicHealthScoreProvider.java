package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Default heuristic implementation of {@link HealthScoreProvider}.
 *
 * <p>Blends three components already available on the {@link InsightContext} so no extra queries
 * are needed:</p>
 * <ul>
 *   <li><b>Budget adherence</b> (0–40): penalised as category budgets approach/exceed their limits.</li>
 *   <li><b>Savings behaviour</b> (0–30): penalised the longer it has been since the user saved.</li>
 *   <li><b>Debt load</b> (0–30): penalised by outstanding balances and unsettled settlements.</li>
 * </ul>
 *
 * <p>The result is clamped to [0, 100]. This is intentionally simple and transparent; a richer
 * Health Score Engine can replace it without affecting any caller.</p>
 */
@Component
public class HeuristicHealthScoreProvider implements HealthScoreProvider {

    @Override
    public int scoreFor(InsightContext context) {
        int score = budgetComponent(context) + savingsComponent(context) + debtComponent(context);
        return Math.max(0, Math.min(100, score));
    }

    /** 40 when every budget is comfortably within limits, degrading toward 0 as usage climbs. */
    private int budgetComponent(InsightContext context) {
        List<BudgetUsageResponse> usages = context.getBudgetUsages();
        if (usages == null || usages.isEmpty()) {
            return 30; // neutral-ish when there is no budget to judge
        }
        double avgUsage = usages.stream()
                .mapToInt(BudgetUsageResponse::getPercentageUsed)
                .average()
                .orElse(0);
        // 0% used → full 40; 100%+ used → 0.
        double retained = Math.max(0, 1.0 - avgUsage / 100.0);
        return (int) Math.round(40 * retained);
    }

    /** 30 when the user saved recently, falling off after ~90 days without a contribution. */
    private int savingsComponent(InsightContext context) {
        long days = context.daysSinceLastSaving();
        if (days <= 30) {
            return 30;
        }
        if (days >= 120) {
            return 0;
        }
        double retained = 1.0 - (days - 30) / 90.0;
        return (int) Math.round(30 * retained);
    }

    /** 30 when debt-free, degrading with the amount owed and the number of open settlements. */
    private int debtComponent(InsightContext context) {
        BigDecimal owed = context.getTotalOwed();
        int settlements = context.getOwedSettlementCount();
        if ((owed == null || owed.compareTo(BigDecimal.ZERO) <= 0) && settlements == 0) {
            return 30;
        }
        double owedAmount = owed == null ? 0 : owed.doubleValue();
        // Lose ~1 point per ₹2,000 owed and 3 points per open settlement.
        double penalty = owedAmount / 2000.0 + settlements * 3.0;
        return (int) Math.round(Math.max(0, 30 - penalty));
    }
}
