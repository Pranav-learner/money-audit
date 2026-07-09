package com.Pranav.finance_tracker.financialintelligence.healthscore.service;

import com.Pranav.finance_tracker.financialintelligence.healthscore.HealthScoreProvider;
import com.Pranav.finance_tracker.financialintelligence.healthscore.engine.HealthScoreEngine;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The single {@link HealthScoreProvider} bean, backing the seam that recommendations and forecasting
 * depend on with the real {@link HealthScoreEngine}. Consumers get the same overall score the engine
 * persists, keeping the score consistent across every module.
 */
@Component
@RequiredArgsConstructor
public class EngineHealthScoreProvider implements HealthScoreProvider {

    private final HealthScoreEngine engine;

    @Override
    public int scoreFor(InsightContext context) {
        return engine.evaluate(context).getOverallScore();
    }
}
