package com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.FinancialPredictor;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Predictor 3 — Budget Forecast.
 *
 * <p>Estimates whether the user will exceed their overall budget and, if so, by how much. The
 * predicted value is projected budget usage as a percentage; the explanation quantifies any
 * expected overrun in rupees.</p>
 */
@Component
public class BudgetForecastPredictor implements FinancialPredictor {

    @Override
    public ForecastType type() {
        return ForecastType.BUDGET_USAGE;
    }

    @Override
    public ForecastDraft predict(ForecastContext context) {
        BigDecimal totalBudget = context.getInsight().totalBudget();
        if (totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // no budget set — nothing to forecast against
        }

        BigDecimal projectedSpend = context.projectedMonthEndSpend();
        BigDecimal usagePct = projectedSpend
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBudget, 0, RoundingMode.HALF_UP);

        String explanation;
        if (projectedSpend.compareTo(totalBudget) > 0) {
            BigDecimal overrun = projectedSpend.subtract(totalBudget);
            explanation = String.format(
                    "At your current pace you're projected to use %s%% of your %s budget — exceeding it by "
                            + "about %s by month end.",
                    usagePct, MoneyFormatter.rupees(totalBudget), MoneyFormatter.rupees(overrun));
        } else {
            explanation = String.format(
                    "You're projected to use %s%% of your %s budget this month, staying within the limit.",
                    usagePct, MoneyFormatter.rupees(totalBudget));
        }

        return ForecastDraft.builder()
                .forecastType(ForecastType.BUDGET_USAGE)
                .predictedValue(usagePct)
                .confidence(context.progressConfidence())
                .predictionDate(context.monthEnd())
                .predictionPeriod(context.periodLabel())
                .explanation(explanation)
                .build();
    }
}
