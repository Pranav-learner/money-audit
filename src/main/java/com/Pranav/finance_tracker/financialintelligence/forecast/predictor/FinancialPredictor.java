package com.Pranav.finance_tracker.financialintelligence.forecast.predictor;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;

/**
 * A single financial predictor (Strategy pattern).
 *
 * <p>Each predictor is its own Spring bean, discovered automatically by the {@code ForecastEngine}.
 * Adding a prediction means adding a new implementation — no existing class changes (Open/Closed).</p>
 *
 * <p><b>Future ML compatibility:</b> this interface is the seam. A rule-based predictor
 * (e.g. {@code MonthlySpendingPredictor}) can be swapped for a learned model
 * (e.g. {@code MonthlySpendingMLModel}) by providing another bean of the same
 * {@link ForecastType} — the engine, service, DTOs, REST API and schema are untouched.</p>
 */
public interface FinancialPredictor {

    /** The type of forecast this predictor produces. */
    ForecastType type();

    /**
     * Produces a prediction for the user described by {@code context}.
     *
     * @return the forecast draft, or {@code null} when there is not enough data to predict
     */
    ForecastDraft predict(ForecastContext context);
}
