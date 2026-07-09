package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.ForecastService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Exposes explainable forecasts via the Forecasting Engine. */
@Component
@RequiredArgsConstructor
public class ForecastTool implements FinancialTool {

    private final ForecastService forecastService;

    @Override
    public String key() {
        return ToolKeys.FORECAST;
    }

    @Override
    public String moduleLabel() {
        return "Forecasting";
    }

    @Override
    public ToolResult fetch(User user) {
        ForecastSummaryResponse summary = forecastService.getSummary(user);
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, summary == null ? null : summary.getSpendingForecast());
        addIfPresent(parts, summary == null ? null : summary.getSavingsForecast());
        addIfPresent(parts, summary == null ? null : summary.getCashflowForecast());
        addIfPresent(parts, summary == null ? null : summary.getBudgetForecast());

        if (parts.isEmpty()) {
            return ToolResult.unavailable(key(), moduleLabel());
        }
        return ToolResult.of(key(), moduleLabel(), String.join(" ", parts));
    }

    private void addIfPresent(List<String> parts, FinancialForecastResponse forecast) {
        if (forecast != null && forecast.getExplanation() != null) {
            parts.add(forecast.getExplanation());
        }
    }
}
