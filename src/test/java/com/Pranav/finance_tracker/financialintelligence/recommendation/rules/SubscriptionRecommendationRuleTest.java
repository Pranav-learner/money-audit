package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.SubscriptionRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.service.HeuristicRecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionRecommendationRuleTest {

    private final SubscriptionRecommendationRule rule = new SubscriptionRecommendationRule(
            new HeuristicRecurringChargeDetector(new RiskThresholdProperties()), new RecommendationProperties());

    @Test
    void recommendsCancellingAnExpensiveSubscription() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("599", LocalDate.of(2026, month, 5), "Entertainment", "Streaming Plus"));
        }
        InsightContext insight = TestFixtures.riskContext().windowExpenses(window).build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.SUBSCRIPTION);
        assertThat(drafts.get(0).getRuleKey()).isEqualTo("SUBSCRIPTION_CANCEL:Streaming Plus");
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isEqualByComparingTo("599");
        assertThat(drafts.get(0).getDescription()).contains("a year"); // annual saving mentioned
    }

    @Test
    void noRecommendationWithoutRecurringSubscriptions() {
        InsightContext insight = TestFixtures.riskContext()
                .windowExpenses(List.of(TestFixtures.expense("599", LocalDate.of(2026, 6, 5), "Entertainment", "One off")))
                .build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"))).isEmpty();
    }
}
