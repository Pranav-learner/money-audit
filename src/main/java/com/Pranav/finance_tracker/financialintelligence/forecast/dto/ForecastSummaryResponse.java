package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The latest forecast of each type, powering the forecast summary and dashboard trend card.
 * Any field may be {@code null} if that forecast has not yet been generated for the user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastSummaryResponse {

    private FinancialForecastResponse spendingForecast;
    private FinancialForecastResponse savingsForecast;
    private FinancialForecastResponse budgetForecast;
    private FinancialForecastResponse cashflowForecast;
    private FinancialForecastResponse debtForecast;
    private FinancialForecastResponse netWorthForecast;
}
