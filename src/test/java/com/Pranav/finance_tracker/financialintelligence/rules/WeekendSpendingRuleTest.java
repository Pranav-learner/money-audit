package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.WeekendSpendingRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeekendSpendingRuleTest {

    private final WeekendSpendingRule rule = new WeekendSpendingRule();

    // Fixed reference dates (Jan 2026: 1st = Thursday).
    private static final LocalDate SAT_1 = LocalDate.of(2026, 1, 3);
    private static final LocalDate SUN_1 = LocalDate.of(2026, 1, 4);
    private static final LocalDate SAT_2 = LocalDate.of(2026, 1, 10);
    private static final LocalDate MON = LocalDate.of(2026, 1, 5);
    private static final LocalDate TUE = LocalDate.of(2026, 1, 6);
    private static final LocalDate WED = LocalDate.of(2026, 1, 7);

    private InsightContext context(List<Expense> window) {
        LocalDate today = LocalDate.of(2026, 1, 20);
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(window)
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void flagsWhenWeekendAverageConsistentlyExceedsWeekdays() {
        List<Expense> window = List.of(
                TestFixtures.expense("1000", SAT_1, "Entertainment"),
                TestFixtures.expense("1000", SUN_1, "Entertainment"),
                TestFixtures.expense("1000", SAT_2, "Entertainment"),
                TestFixtures.expense("300", MON, "Food"),
                TestFixtures.expense("300", TUE, "Food"),
                TestFixtures.expense("300", WED, "Food"));

        List<InsightDraft> drafts = rule.evaluate(context(window));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getInsightType()).isEqualTo(InsightType.TREND);
        assertThat(draft.getDescription()).contains("₹1,000").contains("₹300");
    }

    @Test
    void noInsightWhenWeekendSpendingIsNotHigher() {
        List<Expense> window = List.of(
                TestFixtures.expense("300", SAT_1, "Food"),
                TestFixtures.expense("300", SUN_1, "Food"),
                TestFixtures.expense("300", SAT_2, "Food"),
                TestFixtures.expense("300", MON, "Food"),
                TestFixtures.expense("300", TUE, "Food"),
                TestFixtures.expense("300", WED, "Food"));

        assertThat(rule.evaluate(context(window))).isEmpty();
    }

    @Test
    void noInsightWithoutEnoughDaysPerGroup() {
        List<Expense> window = List.of(
                TestFixtures.expense("1000", SAT_1, "Entertainment"),
                TestFixtures.expense("300", MON, "Food"));

        assertThat(rule.evaluate(context(window))).isEmpty();
    }
}
