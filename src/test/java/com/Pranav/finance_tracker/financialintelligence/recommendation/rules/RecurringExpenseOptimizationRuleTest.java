package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.RecurringExpenseOptimizationRule;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.service.HeuristicRecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringExpenseOptimizationRuleTest {

    private final RecurringExpenseOptimizationRule rule = new RecurringExpenseOptimizationRule(
            new HeuristicRecurringChargeDetector(new RiskThresholdProperties()), new RecommendationProperties());

    @Test
    void recommendsOptimizingWhenSeveralRecurringBillsExist() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("1000", LocalDate.of(2026, month, 3), "Utilities", "Internet"));
            window.add(TestFixtures.expense("500", LocalDate.of(2026, month, 9), "Utilities", "Cloud Storage"));
        }
        InsightContext insight = TestFixtures.riskContext().windowExpenses(window).build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.SPENDING);
        // 15% of ₹1,500 total = ₹225 → rounded to ₹200.
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isEqualByComparingTo("200");
    }

    @Test
    void noRecommendationWithFewerThanTwoRecurringBills() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("1000", LocalDate.of(2026, month, 3), "Utilities", "Internet"));
        }
        InsightContext insight = TestFixtures.riskContext().windowExpenses(window).build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"))).isEmpty();
    }
}
