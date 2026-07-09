package com.Pranav.finance_tracker.financialintelligence.recommendation.service;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicHealthScoreProviderTest {

    private final HeuristicHealthScoreProvider provider = new HeuristicHealthScoreProvider();

    @Test
    void healthyUserScoresHigh() {
        InsightContext insight = TestFixtures.riskContext()
                .lastSavingDate(LocalDate.now().minusDays(5))
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "1500", "3500", 30, "NORMAL")))
                .totalOwed(BigDecimal.ZERO)
                .owedSettlementCount(0)
                .build();

        assertThat(provider.scoreFor(insight)).isGreaterThan(80);
    }

    @Test
    void strugglingUserScoresLow() {
        InsightContext insight = InsightContext.builder()
                .user(TestFixtures.user())
                .today(LocalDate.now())
                .currentMonth(java.time.YearMonth.now())
                .previousMonth(java.time.YearMonth.now().minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "6000", "-1000", 120, "OVER_BUDGET")))
                .lastSavingDate(LocalDate.now().minusDays(200))
                .totalOwed(new BigDecimal("30000"))
                .owedSettlementCount(6)
                .build();

        assertThat(provider.scoreFor(insight)).isLessThan(30);
    }

    @Test
    void scoreIsAlwaysWithinBounds() {
        InsightContext insight = TestFixtures.riskContext().build();
        int score = provider.scoreFor(insight);
        assertThat(score).isBetween(0, 100);
    }
}
