package com.Pranav.finance_tracker.financialintelligence.forecast.service;

import com.Pranav.finance_tracker.financialintelligence.forecast.dto.FinancialForecastResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.FinancialForecast;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.engine.ForecastEngine;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.forecast.repository.FinancialForecastRepository;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates forecast generation (write side) and retrieval (read side).
 *
 * <p>Generation builds a {@link ForecastContext} over the shared {@link InsightContext}, runs the
 * {@link ForecastEngine} and persists the day's predictions (append-only, de-duplicated per type
 * per day). Reads expose the latest forecast per type for the API and dashboard.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final ForecastContextFactory contextFactory;
    private final ForecastEngine forecastEngine;
    private final FinancialForecastRepository forecastRepository;

    // ── Write side (nightly scheduler) ──────────────────────────────────

    @Transactional
    public int generateForUser(User user) {
        return generateForUser(user, contextFactory.build(user).getInsight());
    }

    /**
     * Generates and persists today's forecasts for one user, reusing the supplied
     * {@link InsightContext} so the nightly pipeline loads the user's data once.
     *
     * @return the number of newly persisted forecasts
     */
    @Transactional
    public int generateForUser(User user, InsightContext insightContext) {
        ForecastContext context = contextFactory.build(user, insightContext);
        List<ForecastDraft> drafts = forecastEngine.generate(context);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<FinancialForecast> toSave = new ArrayList<>();
        for (ForecastDraft draft : drafts) {
            boolean alreadyToday = forecastRepository.existsByUserIdAndForecastTypeAndCreatedAtBetween(
                    user.getId(), draft.getForecastType(), dayStart, dayEnd);
            if (alreadyToday) {
                continue;
            }
            toSave.add(toEntity(user, draft, now));
        }

        if (toSave.isEmpty()) {
            return 0;
        }
        forecastRepository.saveAll(toSave);
        return toSave.size();
    }

    // ── Read side (REST controller) ─────────────────────────────────────

    /** The latest forecast of each type the user has, newest types first. */
    @Transactional(readOnly = true)
    public List<FinancialForecastResponse> getForecasts(User user) {
        return latestByType(user).values().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ForecastSummaryResponse getSummary(User user) {
        Map<ForecastType, FinancialForecast> latest = latestByType(user);
        return ForecastSummaryResponse.builder()
                .spendingForecast(response(latest, ForecastType.MONTHLY_SPENDING))
                .savingsForecast(response(latest, ForecastType.MONTHLY_SAVINGS))
                .budgetForecast(response(latest, ForecastType.BUDGET_USAGE))
                .cashflowForecast(response(latest, ForecastType.CASHFLOW))
                .debtForecast(response(latest, ForecastType.DEBT))
                .netWorthForecast(response(latest, ForecastType.NET_WORTH))
                .build();
    }

    private Map<ForecastType, FinancialForecast> latestByType(User user) {
        Map<ForecastType, FinancialForecast> latest = new EnumMap<>(ForecastType.class);
        // Newest-first stream; keep the first (latest) seen per type.
        for (FinancialForecast f : forecastRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            latest.putIfAbsent(f.getForecastType(), f);
        }
        return latest;
    }

    private FinancialForecastResponse response(Map<ForecastType, FinancialForecast> latest, ForecastType type) {
        FinancialForecast f = latest.get(type);
        return f == null ? null : toResponse(f);
    }

    private FinancialForecast toEntity(User user, ForecastDraft draft, LocalDateTime now) {
        return FinancialForecast.builder()
                .userId(user.getId())
                .forecastType(draft.getForecastType())
                .predictedValue(draft.getPredictedValue())
                .confidence(draft.getConfidence())
                .predictionDate(draft.getPredictionDate())
                .predictionPeriod(draft.getPredictionPeriod())
                .explanation(draft.getExplanation())
                .createdAt(now)
                .build();
    }

    private FinancialForecastResponse toResponse(FinancialForecast entity) {
        return FinancialForecastResponse.builder()
                .id(entity.getId())
                .forecastType(entity.getForecastType())
                .predictedValue(entity.getPredictedValue())
                .confidence(entity.getConfidence())
                .predictionDate(entity.getPredictionDate())
                .predictionPeriod(entity.getPredictionPeriod())
                .explanation(entity.getExplanation())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
