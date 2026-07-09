package com.Pranav.finance_tracker.financialintelligence.rules.impl;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightRule;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule 2 — warns when a category budget is nearly or fully consumed.
 *
 * <ul>
 *   <li>&ge; 100% used → HIGH severity "budget exceeded" alert.</li>
 *   <li>&ge; 80% used → MEDIUM severity "approaching limit" alert.</li>
 * </ul>
 *
 * <p>Emits one draft per qualifying budget. Each draft's dedup key is suffixed with the
 * category name so alerts for different categories coexist, while a repeat of the same
 * category on the same day is suppressed.</p>
 */
@Component
public class BudgetUsageRule implements InsightRule {

    private static final String RULE_KEY = "BUDGET_USAGE";
    private static final int NEAR_LIMIT_PCT = 80;
    private static final int OVER_LIMIT_PCT = 100;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        List<BudgetUsageResponse> usages = context.getBudgetUsages();
        if (usages == null || usages.isEmpty()) {
            return List.of();
        }

        List<InsightDraft> drafts = new ArrayList<>();
        for (BudgetUsageResponse usage : usages) {
            int pct = usage.getPercentageUsed();
            if (pct >= OVER_LIMIT_PCT) {
                drafts.add(overBudget(usage, pct));
            } else if (pct >= NEAR_LIMIT_PCT) {
                drafts.add(nearLimit(usage, pct));
            }
        }
        return drafts;
    }

    private InsightDraft overBudget(BudgetUsageResponse usage, int pct) {
        String description = String.format(
                "You've spent %s of your %s %s budget — that's %s of the limit.",
                MoneyFormatter.rupees(usage.getSpent()),
                MoneyFormatter.rupees(usage.getBudget()),
                usage.getCategory(),
                MoneyFormatter.percent(pct));

        return InsightDraft.builder()
                .ruleKey(scopedKey(usage.getCategory()))
                .title("Budget exceeded: " + usage.getCategory())
                .description(description)
                .insightType(InsightType.BUDGET_ALERT)
                .severity(Severity.HIGH)
                .category(usage.getCategory())
                .actionSuggestion("Pause non-essential " + usage.getCategory().toLowerCase()
                        + " spending for the rest of the month to get back on track.")
                .confidence(0.95)
                .build();
    }

    private InsightDraft nearLimit(BudgetUsageResponse usage, int pct) {
        String description = String.format(
                "You've used %s of your %s %s budget (%s spent).",
                MoneyFormatter.percent(pct),
                MoneyFormatter.rupees(usage.getBudget()),
                usage.getCategory(),
                MoneyFormatter.rupees(usage.getSpent()));

        return InsightDraft.builder()
                .ruleKey(scopedKey(usage.getCategory()))
                .title("Approaching your " + usage.getCategory() + " budget")
                .description(description)
                .insightType(InsightType.BUDGET_ALERT)
                .severity(Severity.MEDIUM)
                .category(usage.getCategory())
                .actionSuggestion("You have " + MoneyFormatter.rupees(usage.getRemaining())
                        + " left — spread it across the days remaining this month.")
                .confidence(0.9)
                .build();
    }

    private String scopedKey(String category) {
        return RULE_KEY + ":" + category;
    }
}
