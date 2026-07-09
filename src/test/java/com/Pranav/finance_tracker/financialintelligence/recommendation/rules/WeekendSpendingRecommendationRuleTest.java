package com.Pranav.finance_tracker.financialintelligence.recommendation.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.RecoFixtures;
import com.Pranav.finance_tracker.financialintelligence.recommendation.config.RecommendationProperties;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationDraft;
import com.Pranav.finance_tracker.financialintelligence.recommendation.entity.RecommendationType;
import com.Pranav.finance_tracker.financialintelligence.recommendation.rules.impl.WeekendSpendingRecommendationRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeekendSpendingRecommendationRuleTest {

    private final WeekendSpendingRecommendationRule rule =
            new WeekendSpendingRecommendationRule(new RecommendationProperties());

    @Test
    void recommendsReductionWhenWeekendsAreExpensive() {
        // Saturdays/Sundays in July 2026 at ₹2,000/day vs weekdays at ₹500/day.
        List<Expense> window = List.of(
                TestFixtures.expense("2000", LocalDate.of(2026, 7, 4), "Leisure"),   // Sat
                TestFixtures.expense("2000", LocalDate.of(2026, 7, 5), "Leisure"),   // Sun
                TestFixtures.expense("2000", LocalDate.of(2026, 7, 11), "Leisure"),  // Sat
                TestFixtures.expense("500", LocalDate.of(2026, 7, 6), "Food"),       // Mon
                TestFixtures.expense("500", LocalDate.of(2026, 7, 7), "Food"),       // Tue
                TestFixtures.expense("500", LocalDate.of(2026, 7, 8), "Food"));      // Wed
        InsightContext insight = TestFixtures.riskContext().windowExpenses(window).build();

        List<RecommendationDraft> drafts = rule.evaluate(RecoFixtures.context(insight, 60, "0", "0"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRecommendationType()).isEqualTo(RecommendationType.HABIT);
        assertThat(drafts.get(0).getExpectedMonthlySaving()).isPositive();
    }

    @Test
    void noRecommendationWhenWeekendsAreOrdinary() {
        List<Expense> window = List.of(
                TestFixtures.expense("500", LocalDate.of(2026, 7, 4), "Leisure"),
                TestFixtures.expense("500", LocalDate.of(2026, 7, 5), "Leisure"),
                TestFixtures.expense("500", LocalDate.of(2026, 7, 11), "Leisure"),
                TestFixtures.expense("500", LocalDate.of(2026, 7, 6), "Food"),
                TestFixtures.expense("500", LocalDate.of(2026, 7, 7), "Food"),
                TestFixtures.expense("500", LocalDate.of(2026, 7, 8), "Food"));
        InsightContext insight = TestFixtures.riskContext().windowExpenses(window).build();

        assertThat(rule.evaluate(RecoFixtures.context(insight, 80, "0", "0"))).isEmpty();
    }
}
