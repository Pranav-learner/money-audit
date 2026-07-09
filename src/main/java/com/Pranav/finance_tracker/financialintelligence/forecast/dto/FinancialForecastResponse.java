package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Client-facing view of a {@code FinancialForecast}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialForecastResponse {

    private UUID id;
    private ForecastType forecastType;
    private BigDecimal predictedValue;
    private double confidence;
    private LocalDate predictionDate;
    private String predictionPeriod;
    private String explanation;
    private LocalDateTime createdAt;
}
