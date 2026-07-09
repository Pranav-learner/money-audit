package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.SpendingSpikeRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpendingSpikeRuleTest {

    private final SpendingSpikeRule rule = new SpendingSpikeRule(new RiskThresholdProperties());

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    private InsightContext context(List<Expense> window) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(TODAY)
                .currentMonth(YearMonth.from(TODAY))
                .previousMonth(YearMonth.from(TODAY).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void detectsWeeklySpikeAgainstFourWeekBaseline() {
        // Baseline: ₹1,000/week over the prior four weeks; this week ₹5,000 → 5× spike.
        var window = List.of(
                TestFixtures.expense("1000", LocalDate.of(2026, 6, 20), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 6, 27), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 7, 4), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 7, 11), "Shopping"),
                TestFixtures.expense("5000", LocalDate.of(2026, 7, 18), "Shopping"));

        List<InsightDraft> drafts = rule.evaluate(context(window));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRiskType()).isEqualTo(FinancialRiskType.SPENDING_SPIKE);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(drafts.get(0).getDescription()).contains("this week");
    }

    @Test
    void noSpikeWhenThisWeekIsInLineWithBaseline() {
        var window = List.of(
                TestFixtures.expense("1000", LocalDate.of(2026, 6, 20), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 6, 27), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 7, 4), "Shopping"),
                TestFixtures.expense("1000", LocalDate.of(2026, 7, 11), "Shopping"),
                TestFixtures.expense("1100", LocalDate.of(2026, 7, 18), "Shopping"));

        assertThat(rule.evaluate(context(window))).isEmpty();
    }

    @Test
    void noSpikeWithoutBaseline() {
        var window = List.of(TestFixtures.expense("5000", LocalDate.of(2026, 7, 18), "Shopping"));
        assertThat(rule.evaluate(context(window))).isEmpty();
    }
}
