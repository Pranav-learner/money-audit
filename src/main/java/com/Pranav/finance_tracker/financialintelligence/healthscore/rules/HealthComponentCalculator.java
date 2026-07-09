package com.Pranav.finance_tracker.financialintelligence.healthscore.rules;

import com.Pranav.finance_tracker.financialintelligence.healthscore.dto.ComponentScore;
import com.Pranav.finance_tracker.financialintelligence.healthscore.entity.HealthComponent;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

/**
 * Calculates one {@link HealthComponent}'s contribution to the health score (Strategy pattern).
 *
 * <p>Each calculator is an independent Spring bean discovered by the {@code HealthScoreEngine}, so a
 * component's scoring can be re-tuned or replaced by an ML model without touching the engine or the
 * other components (Open/Closed).</p>
 */
public interface HealthComponentCalculator {

    /** The component this calculator scores. */
    HealthComponent component();

    /**
     * Scores the component from the preloaded context.
     *
     * @return the component score (0 … its configured weight) with an explanation
     */
    ComponentScore evaluate(InsightContext context);
}
