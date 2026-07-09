package com.Pranav.finance_tracker.financialintelligence.forecast.engine;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link FinancialPredictor} against a context and collects the forecasts.
 *
 * <p>Spring injects <b>all</b> predictor beans (Strategy pattern), so adding or swapping a
 * predictor — rule-based or ML — requires no change here. A predictor that returns {@code null}
 * (insufficient data) is skipped; a predictor that throws is logged and isolated so the rest still
 * run. Persistence, insight generation and notification happen downstream in the service.</p>
 */
@Component
@Slf4j
public class ForecastEngine {

    private final List<FinancialPredictor> predictors;

    public ForecastEngine(List<FinancialPredictor> predictors) {
        this.predictors = predictors;
        log.info("ForecastEngine initialised with {} predictor(s): {}", predictors.size(),
                predictors.stream().map(p -> p.type().name()).toList());
    }

    /**
     * Runs all predictors and returns every forecast they produce (nulls skipped).
     *
     * @param context preloaded, health-scored data for one user
     * @return all forecast drafts (never {@code null})
     */
    public List<ForecastDraft> generate(ForecastContext context) {
        List<ForecastDraft> forecasts = new ArrayList<>();
        for (FinancialPredictor predictor : predictors) {
            try {
                ForecastDraft draft = predictor.predict(context);
                if (draft != null) {
                    forecasts.add(draft);
                }
            } catch (Exception ex) {
                Object userId = context.getInsight() != null && context.getInsight().getUser() != null
                        ? context.getInsight().getUser().getId() : "unknown";
                log.error("Predictor '{}' failed for user {}: {}", predictor.type(), userId, ex.getMessage(), ex);
            }
        }
        return forecasts;
    }
}
