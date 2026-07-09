package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.FoodSpendingRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodSpendingRecommendationRuleTest {

    private final FoodSpendingRecommendationRule rule = new FoodSpendingRecommendationRule(new RecommendationProperties());

    @Test
    void recommendsCuttingDiningWhenFoodSpendJumped() {
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("5000", LocalDate.now(), "Food")))
                .previousMonthExpenses(List.of(TestFixtures.expense("3000", LocalDate.now().minusMonths(1), "Food")))
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"));

        assertThat(drafts).hasSize(1);
        RecommendationDraft draft = drafts.get(0);
        assertThat(draft.getRecommendationType()).isEqualTo(RecommendationType.SPENDING);
        assertThat(draft.getExpectedMonthlySaving()).isEqualByComparingTo("1000"); // half of the 2000 increase
        assertThat(draft.getDescription()).contains("restaurant");
    }

    @Test
    void noRecommendationWhenFoodSpendStable() {
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("3100", LocalDate.now(), "Food")))
                .previousMonthExpenses(List.of(TestFixtures.expense("3000", LocalDate.now().minusMonths(1), "Food")))
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"))).isEmpty();
    }

    @Test
    void noRecommendationWithoutBaseline() {
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("5000", LocalDate.now(), "Food")))
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"))).isEmpty();
    }
}
