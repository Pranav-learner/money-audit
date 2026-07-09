package com.Pranav.finance_tracker.financialintelligence.healthscore.service;

import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.healthscore.config.HealthScoreProperties;
import com.Pranav.finance_tracker.financialintelligence.healthscore.engine.HealthScoreEngine;
import com.Pranav.finance_tracker.financialintelligence.healthscore.rules.impl.*;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngineHealthScoreProviderTest {

    private final HealthScoreProperties props = new HealthScoreProperties();
    private final HealthScoreEngine engine = new HealthScoreEngine(List.of(
            new BudgetAdherenceCalculator(props), new SavingsBehaviorCalculator(props),
            new DebtManagementCalculator(props), new SpendingStabilityCalculator(props)));
    private final EngineHealthScoreProvider provider = new EngineHealthScoreProvider(engine);

    @Test
    void providerReturnsEngineOverallScoreInRange() {
        InsightContext ctx = TestFixtures.riskContext()
                .budgetUsages(List.of(TestFixtures.budgetUsage("Food", "5000", "1500", "3500", 30, "NORMAL")))
                .lastSavingDate(LocalDate.now().minusDays(3))
                .build();

        int viaProvider = provider.scoreFor(ctx);
        int viaEngine = engine.evaluate(ctx).getOverallScore();

        assertThat(viaProvider).isEqualTo(viaEngine).isBetween(0, 100);
    }
}
