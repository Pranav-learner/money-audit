package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Predictor 2 — Monthly Savings Forecast.
 *
 * <p>Projects savings for the month from the current savings run-rate. When nothing has been saved
 * yet, it still emits a (zero) forecast so the user sees the gap explicitly.</p>
 */
@Component
public class MonthlySavingsPredictor implements FinancialPredictor {

    @Override
    public ForecastType type() {
        return ForecastType.MONTHLY_SAVINGS;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal saved = context.getSavedThisMonth() == null ? BigDecimal.ZERO : context.getSavedThisMonth();
        BigDecimal projected = context.projectedMonthEndSavings();

        String explanation = saved.compareTo(BigDecimal.ZERO) <= 0
                ? String.format("You haven't recorded any savings yet in %s. On current behaviour you're "
                        + "projected to save little this month.", context.periodLabel())
                : String.format("You've saved %s so far this month. At this rate you're projected to save "
                        + "about %s by month end.", MoneyFormatter.rupees(saved), MoneyFormatter.rupees(projected));

        return ForecastDraft.builder()
                .forecastType(ForecastType.MONTHLY_SAVINGS)
                .predictedValue(projected)
                .confidence(context.progressConfidence())
                .predictionDate(context.monthEnd())
                .predictionPeriod(context.periodLabel())
                .explanation(explanation)
                .build();
    }
}
