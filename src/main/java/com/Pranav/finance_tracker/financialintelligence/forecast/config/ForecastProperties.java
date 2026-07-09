package com.Pranav.finance_tracker.financialintelligence.forecast.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externally-tunable knobs for the Forecasting &amp; Planning Engine, bound from {@code forecast.*}
 * in {@code application.yml}. Predictors, the planner and the services read these via constructor
 * injection, so behaviour is re-tunable per deployment without code changes.
 */
@Component
@ConfigurationProperties(prefix = "forecast")
@Data
public class ForecastProperties {

    /** Trailing months of history used to establish baselines for predictions. */
    private int forecastWindowMonths = 6;

    /** Success probability at/below which a goal is treated as at-risk of being missed. */
    private double goalConfidenceThreshold = 0.5;

    /** Longest target horizon (months) the planner will suggest for a realistic date. */
    private int maxGoalDurationMonths = 120;

    /** Days of forecast history retained per type before older rows may be pruned. */
    private int predictionHistoryDays = 90;

    /**
     * Progress fraction (0–1) that triggers a positive goal-progress notification
     * (e.g. 0.8 → notify when a goal reaches 80% complete).
     */
    private double notificationProgressThreshold = 0.8;

    /** Months of delay in a goal's projection beyond which the user is warned. */
    private int goalDelayNotificationMonths = 2;
}
