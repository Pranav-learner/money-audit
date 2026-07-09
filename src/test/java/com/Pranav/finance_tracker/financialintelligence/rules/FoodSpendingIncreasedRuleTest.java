package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.FoodSpendingIncreasedRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodSpendingIncreasedRuleTest {

    private final FoodSpendingIncreasedRule rule = new FoodSpendingIncreasedRule();

    private InsightContext context(List<com.Pranav.finance_tracker.expense.entity.Expense> current,
                                   List<com.Pranav.finance_tracker.expense.entity.Expense> previous) {
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
    void flagsSignificantFoodIncreaseWithRealNumbers() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("6450", today, "Food"));
        var previous = List.of(TestFixtures.expense("5000", today.minusMonths(1), "Food"));

        List<InsightDraft> drafts = rule.evaluate(context(current, previous));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getInsightType()).isEqualTo(InsightType.SPENDING_WARNING);
        assertThat(draft.getSeverity()).isEqualTo(Severity.MEDIUM); // 29% increase
        assertThat(draft.getCategory()).isEqualTo("Food");
        assertThat(draft.getDescription()).contains("₹6,450").contains("29%");
    }

    @Test
    void escalatesToHighSeverityForLargeIncrease() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("10000", today, "Food"));
        var previous = List.of(TestFixtures.expense("5000", today.minusMonths(1), "Food"));

        List<InsightDraft> drafts = rule.evaluate(context(current, previous));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.HIGH); // 100% increase
    }

    @Test
    void noInsightWhenIncreaseIsBelowThreshold() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("5200", today, "Food"));   // +4%
        var previous = List.of(TestFixtures.expense("5000", today.minusMonths(1), "Food"));

        assertThat(rule.evaluate(context(current, previous))).isEmpty();
    }

    @Test
    void noInsightWhenNoPreviousBaseline() {
        LocalDate today = LocalDate.now();
        var current = List.of(TestFixtures.expense("6450", today, "Food"));

        assertThat(rule.evaluate(context(current, List.of()))).isEmpty();
    }
}
