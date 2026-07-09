package com.Pranav.finance_tracker.financialintelligence.healthscore.rules;

import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl.*;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealthComponentCalculatorsTest {

    private final HealthScoreProperties props = new HealthScoreProperties();

    @Test
    void budgetAdherenceScoresHighWhenBudgetsHealthy() {
        var ctx = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "1500", "3500", 30, "NORMAL")))
                .build();
        ComponentScore score = new BudgetAdherenceCalculator(props).evaluate(ctx);
        assertThat(score.getScore()).isEqualTo(21); // 30 × (1 - 0.30)
        assertThat(score.getMaxPoints()).isEqualTo(30);
    }

    @Test
    void budgetAdherenceScoresLowWhenOverBudget() {
        var ctx = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "6000", "-1000", 120, "OVER_BUDGET")))
                .build();
        assertThat(new BudgetAdherenceCalculator(props).evaluate(ctx).getScore()).isZero();
    }

    @Test
    void savingsBehaviorFullWhenRecentZeroWhenStale() {
        var recent = TestFixtures.riskContext().lastSavingDate(LocalDate.now().minusDays(5)).build();
        var stale = TestFixtures.riskContext().lastSavingDate(LocalDate.now().minusDays(200)).build();

        assertThat(new SavingsBehaviorCalculator(props).evaluate(recent).getScore()).isEqualTo(30);
        assertThat(new SavingsBehaviorCalculator(props).evaluate(stale).getScore()).isZero();
    }

    @Test
    void debtManagementFullWhenDebtFreeLowWhenHeavy() {
        var debtFree = TestFixtures.riskContext().build();
        var heavy = TestFixtures.riskContext()
                .totalOwed(new BigDecimal("25000")).owedSettlementCount(4).build();

        assertThat(new DebtManagementCalculator(props).evaluate(debtFree).getScore()).isEqualTo(25);
        assertThat(new DebtManagementCalculator(props).evaluate(heavy).getScore()).isLessThan(5);
    }

    @Test
    void spendingStabilityHighWhenSpendingIsSteady() {
        LocalDate now = LocalDate.now();
        List<Expense> window = List.of(
                TestFixtures.expense("1000", now.minusMonths(1), "Food"),
                TestFixtures.expense("1000", now.minusMonths(2), "Food"),
                TestFixtures.expense("1000", now.minusMonths(3), "Food"));
        var ctx = TestFixtures.riskContext().windowExpenses(window).build();

        assertThat(new SpendingStabilityCalculator(props).evaluate(ctx).getScore()).isEqualTo(15);
    }

    @Test
    void spendingStabilityIsNeutralWithoutEnoughHistory() {
        var ctx = TestFixtures.riskContext().build(); // empty window
        ComponentScore score = new SpendingStabilityCalculator(props).evaluate(ctx);
        assertThat(score.getScore()).isEqualTo(9); // 15 × 0.6 neutral
    }
}
