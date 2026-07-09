package com.Pranav.finance_tracker.financialintelligence.forecast.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single explainable prediction produced by the Forecasting Engine.
 *
 * <p>References its owner by {@code userId} only, keeping the module self-contained. Forecasts are
 * append-only history: each nightly run inserts the day's predictions, and reads take the latest
 * per {@link ForecastType}. The {@code explanation} field makes every prediction transparent —
 * a hard requirement of the module.</p>
 */
@Entity
@Table(
        name = "financial_forecasts",
        indexes = {
                @Index(name = "idx_forecast_user_type_created", columnList = "user_id, forecast_type, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_type", nullable = false, length = 40)
    private ForecastType forecastType;

    /** The predicted quantity (₹ for monetary forecasts, or a percentage for budget usage). */
    @Column(name = "predicted_value", precision = 15, scale = 2)
    private BigDecimal predictedValue;

    /** Confidence in the prediction, in [0.0, 1.0]. */
    @Column(nullable = false)
    private double confidence;

    /** The date the prediction targets (e.g. month end). */
    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    /** Human-readable period the prediction covers (e.g. {@code 2026-07} or {@code MONTH_END}). */
    @Column(name = "prediction_period", nullable = false, length = 40)
    private String predictionPeriod;

    /** Plain-language explanation of how the prediction was derived (predictions must be explainable). */
    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
