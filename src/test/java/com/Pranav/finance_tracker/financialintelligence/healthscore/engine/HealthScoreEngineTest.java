package com.Pranav.finance_tracker.financialintelligence.healthscore.engine;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.HealthScoreResult;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthBand;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.HealthComponentCalculator;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl.*;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealthScoreEngineTest {

    private final HealthScoreProperties props = new HealthScoreProperties();

    private HealthScoreEngine realEngine() {
        return new HealthScoreEngine(List.of(
                new BudgetAdherenceCalculator(props), new SavingsBehaviorCalculator(props),
                new DebtManagementCalculator(props), new SpendingStabilityCalculator(props)));
    }

    @Test
    void healthyUserScoresHighWithExplanation() {
        InsightContext ctx = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "1500", "3500", 30, "NORMAL")))
                .lastSavingDate(LocalDate.now().minusDays(3))
                .build();

        HealthScoreResult result = realEngine().evaluate(ctx);

        assertThat(result.getOverallScore()).isGreaterThan(75);
        assertThat(result.getBand()).isIn(HealthBand.GOOD, HealthBand.EXCELLENT);
        assertThat(result.getExplanation()).contains("/100");
        assertThat(result.getComponents()).hasSize(4);
    }

    @Test
    void strugglingUserScoresLow() {
        InsightContext ctx = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "6500", "-1500", 130, "OVER_BUDGET")))
                .lastSavingDate(LocalDate.now().minusDays(200))
                .totalOwed(new BigDecimal("40000")).owedSettlementCount(6)
                .build();

        HealthScoreResult result = realEngine().evaluate(ctx);

        assertThat(result.getOverallScore()).isLessThan(35);
        assertThat(result.getBand()).isIn(HealthBand.NEEDS_ATTENTION, HealthBand.CRITICAL, HealthBand.FAIR);
    }

    @Test
    void scoreIsClampedTo100AndFailingComponentIsIsolated() {
        HealthComponentCalculator huge = new HealthComponentCalculator() {
            @Override
            public HealthComponent component() {
                return HealthComponent.BUDGET_ADHERENCE;
            }

            @Override
            public ComponentScore evaluate(InsightContext context) {
                return ComponentScore.builder().component(component()).maxPoints(200).score(200).reason("x").build();
            }
        };
        HealthComponentCalculator boom = new HealthComponentCalculator() {
            @Override
            public HealthComponent component() {
                return HealthComponent.DEBT_MANAGEMENT;
            }

            @Override
            public ComponentScore evaluate(InsightContext context) {
                throw new IllegalStateException("boom");
            }
        };

        HealthScoreResult result = new HealthScoreEngine(List.of(huge, boom))
                .evaluate(TestFixtures.riskContext().build());

        assertThat(result.getOverallScore()).isEqualTo(100); // clamped
        assertThat(result.getComponents()).hasSize(2); // failing one still recorded (as 0)
    }
}
