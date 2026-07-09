package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.BudgetRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetRecommendationRuleTest {

    private final BudgetRecommendationRule rule = new BudgetRecommendationRule();

    @Test
    void recommendsRebalanceForOverspentBudget() {
        InsightContext insight = TestFixtures.riskContext()
                .budgetUsages(List.of(
                        TestFixtures.budgetUsage("Food", "5000", "5500", "-500", 110, "OVER_BUDGET"),
                        TestFixtures.budgetUsage("Transport", "2000", "1500", "500", 75, "NORMAL")))
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 55, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.BUDGET);
        assertThat(drafts.get(0).getRuleKey()).isEqualTo("BUDGET_ADJUST:Food");
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isEqualByComparingTo("500");
    }

    @Test
    void noRecommendationWhenBudgetsWithinLimits() {
        InsightContext insight = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "4000", "1000", 80, "NEAR_LIMIT")))
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 70, "0", "0"))).isEmpty();
    }
}
