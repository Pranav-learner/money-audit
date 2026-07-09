package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.Priority;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.EmergencyFundRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyFundRecommendationRuleTest {

    private final EmergencyFundRecommendationRule rule = new EmergencyFundRecommendationRule(new RecommendationProperties());

    @Test
    void recommendsContributionWhenFundIsShort() {
        // Monthly spend ₹10,000 → 6-month target ₹60,000; only ₹6,000 saved → HIGH priority.
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("10000", LocalDate.now(), "Food")))
                .build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 40, "6000", "0"));

        assertThat(drafts).hasSize(1);
        RecommendationDraft draft = drafts.get(0);
        assertThat(draft.getRecommendationType()).isEqualTo(RecommendationType.GOAL);
        assertThat(draft.getPriority()).isEqualTo(Priority.HIGH); // cushion below one month of spend
        // gap 54,000 / 12 = 4,500.
        assertThat(draft.getExpectedMonthlySaving()).isEqualByComparingTo("4500");
    }

    @Test
    void noRecommendationWhenAlreadyFunded() {
        InsightContext insight = TestFixtures.riskContext()
                .currentMonthExpenses(List.of(TestFixtures.expense("5000", LocalDate.now(), "Food")))
                .build();

        // Target = 30,000; saved 40,000 → funded.
        assertThat(rule.evaluate(RecoFixtures.context(insight, 90, "40000", "0"))).isEmpty();
    }
}
