package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable prediction produced by a {@code FinancialPredictor} before persistence.
 */
@Getter
@Builder
public class ForecastDraft {

    private final ForecastType forecastType;
    private final BigDecimal predictedValue;
    private final double confidence;
    private final LocalDate predictionDate;
    private final String predictionPeriod;
    private final String explanation;
}
