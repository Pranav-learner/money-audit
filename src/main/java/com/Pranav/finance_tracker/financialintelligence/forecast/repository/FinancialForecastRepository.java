package com.Pranav.finance_tracker.financialintelligence.forecast.repository;

import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialForecast;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Persistence for {@link FinancialForecast}, scoped by {@code userId}. Forecasts are append-only
 * history; reads take the most recent per type.
 */
@Repository
public interface FinancialForecastRepository extends JpaRepository<FinancialForecast, UUID> {

    /** Idempotency guard for the nightly generator: has this forecast type already run today? */
    boolean existsByUserIdAndForecastTypeAndCreatedAtBetween(
            UUID userId, ForecastType forecastType, LocalDateTime start, LocalDateTime end);

    /** All forecasts for a user, newest first. */
    List<FinancialForecast> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** The latest forecast of a given type, if any. */
    FinancialForecast findFirstByUserIdAndForecastTypeOrderByCreatedAtDesc(UUID userId, ForecastType forecastType);
}
