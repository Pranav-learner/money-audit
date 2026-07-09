package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.CashflowRiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowRiskRuleTest {

    private final CashflowRiskRule rule = new CashflowRiskRule(new RiskThresholdProperties());

    /** 20th of a 31-day month, giving a trustworthy run-rate. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    private InsightContext context(List<Expense> currentExpenses, String budget) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(TODAY)
                .currentMonth(YearMonth.from(TODAY))
                .previousMonth(YearMonth.from(TODAY).minusMonths(1))
                .currentMonthExpenses(currentExpenses)
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of(TestFixtures.budgetUsage("All", budget, "0", budget, 0, "NORMAL")))
                .build();
    }

    @Test
    void highRiskWhenProjectedSpendExceedsBudget() {
        // ₹8,000 spent by day 20 → projected ≈ ₹12,400 for the month, over a ₹10,000 budget.
        var expenses = List.of(TestFixtures.expense("8000", LocalDate.of(2026, 7, 10), "Food"));
        List<InsightDraft> drafts = rule.evaluate(context(expenses, "10000"));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(drafts.get(0).getRiskType()).isEqualTo(FinancialRiskType.CASHFLOW_RISK);
        assertThat(drafts.get(0).getDescription()).contains("exceed your monthly budget");
    }

    @Test
    void noRiskWhenProjectionStaysWithinBudget() {
        // ₹3,000 spent by day 20 → projected ≈ ₹4,650, comfortably under ₹10,000.
        var expenses = List.of(TestFixtures.expense("3000", LocalDate.of(2026, 7, 10), "Food"));
        assertThat(rule.evaluate(context(expenses, "10000"))).isEmpty();
    }

    @Test
    void noRiskWhenNoBudgetConfigured() {
        var expenses = List.of(TestFixtures.expense("8000", LocalDate.of(2026, 7, 10), "Food"));
        assertThat(rule.evaluate(context(expenses, "0"))).isEmpty();
    }

    @Test
    void noRiskTooEarlyInTheMonth() {
        LocalDate early = LocalDate.of(2026, 7, 2);
        InsightContext ctx = InsightContext.builder()
                .user(TestFixtures.user())
                .today(early)
                .currentMonth(YearMonth.from(early))
                .previousMonth(YearMonth.from(early).minusMonths(1))
                .currentMonthExpenses(List.of(TestFixtures.expense("5000", early, "Food")))
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of(TestFixtures.budgetUsage("All", "10000", "0", "10000", 0, "NORMAL")))
                .build();

        assertThat(rule.evaluate(ctx)).isEmpty();
    }
}
