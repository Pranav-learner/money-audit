package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Predictor 1 — Monthly Spending Forecast.
 *
 * <p>Projects total spend at month end by extrapolating the current run-rate
 * ({@code spentSoFar / daysElapsed × daysInMonth}). Fully explainable and cheap; a good candidate
 * to later swap for a seasonal ML model behind the same {@link FinancialPredictor} contract.</p>
 */
@Component
public class MonthlySpendingPredictor implements FinancialPredictor {

    @Override
    public ForecastType type() {
        return ForecastType.MONTHLY_SPENDING;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal spentSoFar = context.spentSoFar();
        if (spentSoFar.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // nothing spent yet — no basis to project
        }
        BigDecimal projected = context.projectedMonthEndSpend();

        String explanation = String.format(
                "You've spent %s in the first %d days of %s. At this rate you're on track to spend "
                        + "about %s by month end.",
                MoneyFormatter.rupees(spentSoFar), context.daysElapsed(), context.periodLabel(),
                MoneyFormatter.rupees(projected));

        return ForecastDraft.builder()
                .forecastType(ForecastType.MONTHLY_SPENDING)
                .predictedValue(projected)
                .confidence(context.progressConfidence())
                .predictionDate(context.monthEnd())
                .predictionPeriod(context.periodLabel())
                .explanation(explanation)
                .build();
    }
}
