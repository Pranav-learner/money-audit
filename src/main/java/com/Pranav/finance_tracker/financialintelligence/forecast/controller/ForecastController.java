package com.Pranav.finance_tracker.financialintelligence.forecast.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.ForecastService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for financial forecasts. Operates on the authenticated user only.
 */
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
@Tag(name = "Forecast", description = "Explainable predictions of the user's financial future")
public class ForecastController {

    private final ForecastService forecastService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List the latest forecast of each type")
    public ResponseEntity<List<FinancialForecastResponse>> getForecasts() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(forecastService.getForecasts(user));
    }

    @GetMapping("/summary")
    @Operation(summary = "Forecast summary: spending, savings, budget, cash flow, debt and net worth")
    public ResponseEntity<ForecastSummaryResponse> getSummary() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(forecastService.getSummary(user));
    }
}
