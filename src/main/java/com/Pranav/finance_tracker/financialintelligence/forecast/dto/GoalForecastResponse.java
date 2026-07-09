package com.Pranav.finance_tracker.financialintelligence.forecast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Forecast-oriented view of a goal: probability of success, projected completion, required savings
 * and the recommendations that would improve the odds. Powers {@code GET /api/goals/{id}/forecast}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalForecastResponse {

    private UUID goalId;
    private double successProbability;
    private LocalDate projectedCompletionDate;
    private BigDecimal requiredMonthlySaving;
    private List<String> recommendations;
}
