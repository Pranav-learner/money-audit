package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Predictor 6 — Net Worth Trend.
 *
 * <p>Projects net worth (savings − debt) forward over a short horizon by adding the projected
 * monthly savings run-rate, indicating whether the user's position is growing or declining.</p>
 */
@Component
public class NetWorthPredictor implements FinancialPredictor {

    private static final int HORIZON_MONTHS = 3;
    private static final String PERIOD = "NEXT_" + HORIZON_MONTHS + "_MONTHS";

    @Override
    public ForecastType type() {
        return ForecastType.NET_WORTH;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal current = context.netWorth();
        BigDecimal monthlySavings = context.projectedMonthEndSavings();
        BigDecimal projected = current.add(monthlySavings.multiply(BigDecimal.valueOf(HORIZON_MONTHS)));

        String direction = projected.compareTo(current) >= 0 ? "grow" : "decline";
        String explanation = String.format(
                "Your net worth (savings minus debt) is about %s. Saving roughly %s a month, it's projected to "
                        + "%s to around %s over the next %d months.",
                MoneyFormatter.rupees(current), MoneyFormatter.rupees(monthlySavings),
                direction, MoneyFormatter.rupees(projected), HORIZON_MONTHS);

        return ForecastDraft.builder()
                .forecastType(ForecastType.NET_WORTH)
                .predictedValue(projected)
                .confidence(0.6)
                .predictionDate(context.today().plusMonths(HORIZON_MONTHS))
                .predictionPeriod(PERIOD)
                .explanation(explanation)
                .build();
    }
}
