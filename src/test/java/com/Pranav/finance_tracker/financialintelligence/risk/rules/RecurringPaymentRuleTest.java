package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.RecurringPaymentRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.HeuristicRecurringChargeDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringPaymentRuleTest {

    private final RiskThresholdProperties props = new RiskThresholdProperties();
    private final RecurringPaymentRule rule =
            new RecurringPaymentRule(new HeuristicRecurringChargeDetector(props), props);

    private InsightContext context(LocalDate today, List<Expense> window) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    private List<Expense> rentHistory() {
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("15000", LocalDate.of(2026, month, 5), "Housing", "Rent"));
        }
        return window;
    }

    @Test
    void remindsWhenRecurringBillIsDueSoonAndUnpaidThisMonth() {
        // Today is 2 July; rent historically lands on the 5th → due in 3 days, not yet paid in July.
        List<InsightDraft> drafts = rule.evaluate(context(LocalDate.of(2026, 7, 2), rentHistory()));

        assertThat(drafts).hasSize(1);
        InsightDraft draft = drafts.get(0);
        assertThat(draft.getRiskType()).isEqualTo(FinancialRiskType.RECURRING_PAYMENT);
        assertThat(draft.getRuleKey()).isEqualTo("RECURRING_PAYMENT:Rent");
        assertThat(draft.getDescription()).contains("Rent").contains("5th");
    }

    @Test
    void noReminderWhenAlreadyPaidThisMonth() {
        var window = rentHistory();
        window.add(TestFixtures.expense("15000", LocalDate.of(2026, 7, 5), "Housing", "Rent"));
        // Today is 8 July, already paid on the 5th.
        assertThat(rule.evaluate(context(LocalDate.of(2026, 7, 8), window))).isEmpty();
    }

    @Test
    void noReminderWhenDueDateIsOutsideTheWindow() {
        // Rent historically lands on the 25th; today is 2 July → due in 23 days, well outside the 5-day window.
        var window = new ArrayList<Expense>();
        for (int month = 4; month <= 6; month++) {
            window.add(TestFixtures.expense("15000", LocalDate.of(2026, month, 25), "Housing", "Rent"));
        }
        assertThat(rule.evaluate(context(LocalDate.of(2026, 7, 2), window))).isEmpty();
    }
}
