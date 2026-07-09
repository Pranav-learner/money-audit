package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.UnusualActivityRule;
import com.Pranav.finance_tracker.financialintelligence.risk.service.StatisticalAnomalyDetector;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusualActivityRuleTest {

    private final UnusualActivityRule rule =
            new UnusualActivityRule(new StatisticalAnomalyDetector(new RiskThresholdProperties()));

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    private InsightContext context(List<Expense> current, List<Expense> window) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(TODAY)
                .currentMonth(YearMonth.from(TODAY))
                .previousMonth(YearMonth.from(TODAY).minusMonths(1))
                .currentMonthExpenses(current)
                .previousMonthExpenses(List.of())
                .windowExpenses(window)
                .budgetUsages(List.of())
                .build();
    }

    @Test
    void flagsAStatisticalOutlier() {
        var window = new ArrayList<Expense>();
        for (int i = 0; i < 12; i++) {
            window.add(TestFixtures.expense("200", LocalDate.of(2026, 5, 1).plusDays(i), "Food"));
        }
        Expense outlier = TestFixtures.expense("8000", LocalDate.of(2026, 7, 5), "Electronics", "New phone");
        window.add(outlier);

        List<InsightDraft> drafts = rule.evaluate(context(List.of(outlier), window));

        assertThat(drafts).isNotEmpty();
        assertThat(drafts).allMatch(d -> d.getRiskType() == FinancialRiskType.UNUSUAL_ACTIVITY);
        assertThat(drafts).anyMatch(d -> d.getDescription().contains("New phone"));
        assertThat(drafts.get(0).getConfidence()).isGreaterThan(0.0);
    }

    @Test
    void flagsRepeatedIdenticalTransactions() {
        var current = new ArrayList<Expense>();
        for (int i = 0; i < 4; i++) {
            current.add(TestFixtures.expense("999", LocalDate.of(2026, 7, 3).plusDays(i), "Shopping", "In-app buy"));
        }

        List<InsightDraft> drafts = rule.evaluate(context(current, current));

        assertThat(drafts).anyMatch(d -> d.getDescription().contains("4 transactions"));
    }

    @Test
    void noAnomalyForOrdinarySpending() {
        var current = List.of(TestFixtures.expense("210", LocalDate.of(2026, 7, 5), "Food"));
        var window = new ArrayList<Expense>();
        for (int i = 0; i < 12; i++) {
            window.add(TestFixtures.expense("200", LocalDate.of(2026, 5, 1).plusDays(i), "Food"));
        }
        assertThat(rule.evaluate(context(current, window))).isEmpty();
    }
}
