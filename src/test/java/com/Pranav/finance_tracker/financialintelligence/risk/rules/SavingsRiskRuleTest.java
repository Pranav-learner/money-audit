package com.Pranav.finance_tracker.financialintelligence.risk.rules;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.risk.FinancialRiskType;
import com.Pranav.finance_tracker.financialintelligence.risk.config.RiskThresholdProperties;
import com.Pranav.finance_tracker.financialintelligence.risk.rules.impl.SavingsRiskRule;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SavingsRiskRuleTest {

    private final SavingsRiskRule rule = new SavingsRiskRule(new RiskThresholdProperties());

    /** Current month = July, so the last completed month is June (baseline = Mar–May). */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    private InsightContext contextWithTrend(List<com.Pranav.finance_tracker.analytics.dto.SavingTrendItem> trend) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(TODAY)
                .currentMonth(YearMonth.from(TODAY))
                .previousMonth(YearMonth.from(TODAY).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of())
                .savingsTrend(trend)
                .build();
    }

    @Test
    void raisesRiskWhenRecentSavingsDropSharply() {
        // Baseline (Mar–May) ≈ ₹5,000; June collapses to ₹1,000 → ~80% drop.
        var trend = List.of(
                TestFixtures.savingTrend(3, "5000"),
                TestFixtures.savingTrend(4, "5000"),
                TestFixtures.savingTrend(5, "5000"),
                TestFixtures.savingTrend(6, "1000"));

        List<InsightDraft> drafts = rule.evaluate(contextWithTrend(trend));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getRiskType()).isEqualTo(FinancialRiskType.SAVINGS_RISK);
        assertThat(drafts.get(0).getDescription()).contains("below your recent average");
    }

    @Test
    void highSeverityWhenSavingStopsEntirely() {
        var trend = List.of(
                TestFixtures.savingTrend(3, "4000"),
                TestFixtures.savingTrend(4, "4000"),
                TestFixtures.savingTrend(5, "4000"),
                TestFixtures.savingTrend(6, "0"));

        List<InsightDraft> drafts = rule.evaluate(contextWithTrend(trend));

        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getSeverity()).isEqualTo(Severity.HIGH);
    }

    @Test
    void noRiskWhenSavingsAreStable() {
        var trend = List.of(
                TestFixtures.savingTrend(3, "5000"),
                TestFixtures.savingTrend(4, "5000"),
                TestFixtures.savingTrend(5, "5000"),
                TestFixtures.savingTrend(6, "5200"));

        assertThat(rule.evaluate(contextWithTrend(trend))).isEmpty();
    }

    @Test
    void noRiskWithoutEnoughHistory() {
        var trend = List.of(TestFixtures.savingTrend(6, "1000"));
        assertThat(rule.evaluate(contextWithTrend(trend))).isEmpty();
    }
}
