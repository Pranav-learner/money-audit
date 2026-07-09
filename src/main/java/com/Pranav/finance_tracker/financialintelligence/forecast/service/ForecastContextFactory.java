package com.Pranav.finance_tracker.financialintelligence.forecast.service;

import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.healthscore.HealthScoreProvider;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Assembles a {@link ForecastContext} on top of the shared {@link InsightContext}, reusing the
 * existing {@link AnalyticsService} (savings) and {@link HealthScoreProvider} (Health Score Engine
 * seam). Centralising this build means both the forecast and goal services — and the nightly
 * scheduler — construct forecast data the same way, with no duplicated calculations.
 */
@Component
@RequiredArgsConstructor
public class ForecastContextFactory {

    private final InsightContextFactory insightContextFactory;
    private final AnalyticsService analyticsService;
    private final HealthScoreProvider healthScoreProvider;

    /** Builds the shared insight context itself (used by the read-side / on-demand paths). */
    public ForecastContext build(User user) {
        return build(user, insightContextFactory.build(user));
    }

    /** Reuses an already-built {@link InsightContext} (used by the nightly scheduler). */
    public ForecastContext build(User user, InsightContext insightContext) {
        BigDecimal totalSavings = nz(analyticsService.getTotalSavings(user).getTotalSavings());
        BigDecimal savedThisMonth = nz(analyticsService
                .getMonthlySavings(user, insightContext.getCurrentMonth().getMonthValue(),
                        insightContext.getCurrentMonth().getYear())
                .getTotalSaved());
        int healthScore = healthScoreProvider.scoreFor(insightContext);

        return ForecastContext.builder()
                .insight(insightContext)
                .totalSavings(totalSavings)
                .savedThisMonth(savedThisMonth)
                .healthScore(healthScore)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
