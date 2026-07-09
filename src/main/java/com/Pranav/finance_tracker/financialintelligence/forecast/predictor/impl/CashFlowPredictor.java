package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Predictor 4 — Cash Flow Forecast.
 *
 * <p>Estimates the expected month-end position: how much of the available funds (the overall budget,
 * used as the spendable ceiling) will remain after projected spending. A positive value is a
 * surplus; a negative value is an expected shortfall.</p>
 */
@Component
public class CashFlowPredictor implements FinancialPredictor {

    @Override
    public ForecastType type() {
        return ForecastType.CASHFLOW;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal available = context.getInsight().totalBudget();
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            // Fall back to money that flowed through this month as the spendable proxy.
            available = context.spentSoFar().add(
                    context.getSavedThisMonth() == null ? BigDecimal.ZERO : context.getSavedThisMonth());
        }
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal spentSoFar = context.spentSoFar();
        BigDecimal projectedSpend = context.projectedMonthEndSpend();
        BigDecimal remainingSpend = projectedSpend.subtract(spentSoFar).max(BigDecimal.ZERO);
        BigDecimal expectedBalance = available.subtract(projectedSpend);

        String outlook = expectedBalance.compareTo(BigDecimal.ZERO) >= 0
                ? String.format("a surplus of about %s", MoneyFormatter.rupees(expectedBalance))
                : String.format("a shortfall of about %s", MoneyFormatter.rupees(expectedBalance.abs()));
        String explanation = String.format(
                "You have about %s spendable this month and are projected to spend %s more (%s total). "
                        + "That leaves %s at month end.",
                MoneyFormatter.rupees(available.subtract(spentSoFar).max(BigDecimal.ZERO)),
                MoneyFormatter.rupees(remainingSpend), MoneyFormatter.rupees(projectedSpend), outlook);

        return ForecastDraft.builder()
                .forecastType(ForecastType.CASHFLOW)
                .predictedValue(expectedBalance)
                .confidence(context.progressConfidence())
                .predictionDate(context.monthEnd())
                .predictionPeriod(context.periodLabel())
                .explanation(explanation)
                .build();
    }
}
