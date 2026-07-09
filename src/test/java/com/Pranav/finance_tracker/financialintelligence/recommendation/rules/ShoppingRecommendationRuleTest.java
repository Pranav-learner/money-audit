package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.ShoppingRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingRecommendationRuleTest {

    private final ShoppingRecommendationRule rule = new ShoppingRecommendationRule(new RecommendationProperties());

    @Test
    void recommendsTargetWhenShoppingSpikes() {
        LocalDate now = LocalDate.now();
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("6000", now, "Shopping")))
                .windowExpenses(List.of(
                        TestFixtures.expense("2000", now.minusMonths(1), "Shopping"),
                        TestFixtures.expense("2000", now.minusMonths(2), "Shopping")))
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 55, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.BUDGET);
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isEqualByComparingTo("4000"); // 6000 - 2000 baseline
    }

    @Test
    void noRecommendationWhenShoppingInLineWithHistory() {
        LocalDate now = LocalDate.now();
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("2100", now, "Shopping")))
                .windowExpenses(List.of(
                        TestFixtures.expense("2000", now.minusMonths(1), "Shopping"),
                        TestFixtures.expense("2000", now.minusMonths(2), "Shopping")))
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 70, "0", "0"))).isEmpty();
    }
}
