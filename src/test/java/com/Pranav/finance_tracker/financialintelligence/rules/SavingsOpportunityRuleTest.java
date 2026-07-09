package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.SavingsOpportunityRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsOpportunityRuleTest {

    private final SavingsOpportunityRule rule = new SavingsOpportunityRule();

    private InsightContext context(List<Expense> current, List<Expense> previous) {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(current)
                .previousMonthExpenses(previous)
                .windowExpenses(current)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void recommendsRealisticSavingForTopGrowingCategory() {
        LocalDate today = LocalDate.now();
        var current = List.of(
                TestFixtures.expense("6450", today, "Food"),
                TestFixtures.expense("1000", today, "Transport"));
        var previous = List.of(
                TestFixtures.expense("5000", today.minusMonths(1), "Food"),
                TestFixtures.expense("900", today.minusMonths(1), "Transport"));

        List<InsightDraft> drafts = rule.evaluate(context(current, previous));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getInsightType()).isEqualTo(InsightType.SAVING_OPPORTUNITY);
        assertThat(draft.getCategory()).isEqualTo("Food");
        // increase 1450 -> half = 725 -> rounded to nearest 100 = 700
        assertThat(draft.getDescription()).contains("₹700");
    }

    @Test
    void noInsightWhenIncreaseIsTrivial() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("5100", today, "Food"));
        var previous = List.of(TestFixtures.expense("5000", today.minusMonths(1), "Food"));

        assertThat(rule.evaluate(context(current, previous))).isEmpty();
    }

    @Test
    void noInsightWhenSpendingDidNotGrow() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("4000", today, "Food"));
        var previous = List.of(TestFixtures.expense("5000", today.minusMonths(1), "Food"));

        assertThat(rule.evaluate(context(current, previous))).isEmpty();
    }
}
