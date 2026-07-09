package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Predictor 5 — Debt Forecast.
 *
 * <p>Projects outstanding debt over a short horizon assuming current behaviour continues (i.e. no
 * new settlements). A steady-state projection is deliberately conservative and fully explainable;
 * a future model could incorporate settlement cadence.</p>
 */
@Component
public class DebtForecastPredictor implements FinancialPredictor {

    /** Horizon (months) over which debt is projected. */
    private static final int HORIZON_MONTHS = 3;
    private static final String PERIOD = "NEXT_" + HORIZON_MONTHS + "_MONTHS";

    @Override
    public ForecastType type() {
        return ForecastType.DEBT;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal owed = context.getInsight().getTotalOwed();
        int settlements = context.getInsight().getOwedSettlementCount();

        BigDecimal predicted = owed == null ? BigDecimal.ZERO : owed;
        String explanation;
        if (predicted.compareTo(BigDecimal.ZERO) <= 0) {
            explanation = "You have no outstanding debt. If this continues, you'll stay debt-free over the next "
                    + HORIZON_MONTHS + " months.";
        } else if (settlements > 0) {
            explanation = String.format(
                    "You owe %s across %d settlement%s. Without action, this debt is likely to persist — and "
                            + "may grow — over the next %d months.",
                    MoneyFormatter.rupees(predicted), settlements, settlements == 1 ? "" : "s", HORIZON_MONTHS);
        } else {
            explanation = String.format(
                    "You owe %s. If you make no settlements, this will remain over the next %d months.",
                    MoneyFormatter.rupees(predicted), HORIZON_MONTHS);
        }

        return ForecastDraft.builder()
                .forecastType(ForecastType.DEBT)
                .predictedValue(predicted)
                .confidence(predicted.compareTo(BigDecimal.ZERO) <= 0 ? 0.9 : 0.6)
                .predictionDate(context.today().plusMonths(HORIZON_MONTHS))
                .predictionPeriod(PERIOD)
                .explanation(explanation)
                .build();
    }
}
