package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.impl.LargeExpenseRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LargeExpenseRuleTest {

    private final LargeExpenseRule rule = new LargeExpenseRule();

    private InsightContext context(List<Expense> current, List<Expense> window) {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(current)
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void flagsExpenseFarAboveHistoricalAverage() {
        LocalDate today = LocalDate.now();
        List<Expense> window = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            window.add(TestFixtures.expense("100", today.minusMonths(2).plusDays(i), "Shopping"));
        }
        Expense big = TestFixtures.expense("2000", today, "Shopping", "Laptop");
        window.add(big);

        List<InsightDraft> drafts = rule.evaluate(context(List.of(big), window));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getInsightType()).isEqualTo(InsightType.SPENDING_WARNING);
        assertThat(draft.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(draft.getDescription()).contains("₹2,000").contains("'Laptop'");
    }

    @Test
    void noInsightWhenNotEnoughHistory() {
        LocalDate today = LocalDate.now();
        Expense big = TestFixtures.expense("2000", today, "Shopping", "Laptop");
        // Only 2 expenses total -> below the minimum sample size.
        List<Expense> window = List.of(TestFixtures.expense("100", today.minusDays(3), "Food"), big);

        assertThat(rule.evaluate(context(List.of(big), window))).isEmpty();
    }

    @Test
    void noInsightWhenSpendingIsUniform() {
        LocalDate today = LocalDate.now();
        List<Expense> window = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            window.add(TestFixtures.expense("100", today.minusDays(i), "Food"));
        }
        assertThat(rule.evaluate(context(window, window))).isEmpty();
    }
}
