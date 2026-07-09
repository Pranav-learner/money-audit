package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.SavingsRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsRecommendationRuleTest {

    private final SavingsRecommendationRule rule = new SavingsRecommendationRule(new RecommendationProperties());

    @Test
    void recommendsAdditionalSavingWhenRateLow() {
        // Spent ₹9,500, saved ₹500 → inflow ₹10,000, rate 5% (target 20%).
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("9500", LocalDate.now(), "Food")))
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 50, "0", "500"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.SAVING);
        // target 20% of 10,000 = 2,000; additional = 1,500.
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isEqualByComparingTo("1500");
    }

    @Test
    void noRecommendationWhenAlreadySavingEnough() {
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("6000", LocalDate.now(), "Food")))
                .build();

        // Saved ₹4,000 of ₹10,000 inflow = 40% > 20% target.
        assertThat(rule.evaluate(RecoFixtures.context(insight, 80, "0", "4000"))).isEmpty();
    }
}
