package com.Pranav.finance_tracker.financialintelligence.scheduler;

import com.Pranav.finance_tracker.financialintelligence.forecast.service.ForecastService;
import com.Pranav.finance_tracker.financialintelligence.forecast.service.GoalService;
import com.Pranav.finance_tracker.financialintelligence.healthscore.service.HealthScoreService;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationService;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.financialintelligence.service.FinancialInsightService;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly driver for the Financial Intelligence Engine.
 *
 * <p>Runs at 02:00 every day and, for each user, executes the full pipeline in order:
 * <b>Spending Intelligence → Risk Detection → Recommendations</b>. The user's financial data is
 * loaded <b>once</b> per night (a single {@link InsightContext}) and shared across all phases to
 * avoid duplicate queries. Each phase runs in its own transaction (inside the respective service),
 * so a failure for one user or one phase is logged and isolated without aborting the whole run.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialInsightScheduler {

    private final UserRepository userRepository;
    private final InsightContextFactory contextFactory;
    private final FinancialInsightService insightService;
    private final HealthScoreService healthScoreService;
    private final RecommendationService recommendationService;
    private final ForecastService forecastService;
    private final GoalService goalService;

    /** Every night at 2 AM: {@code second minute hour day-of-month month day-of-week}. */
    @Scheduled(cron = "0 0 2 * * *")
    public void generateNightlyInsights() {
        long startNanos = System.nanoTime();
        List<User> users = userRepository.findAll();
        log.info("[FinancialIntelligence] Nightly run started (Spending Intelligence + Health Score + "
                + "Risk Detection + Recommendations + Forecasting + Goal Planning) for {} user(s)", users.size());

        int usersProcessed = 0;
        int insightsGenerated = 0;
        int healthScoresGenerated = 0;
        int recommendationsGenerated = 0;
        int forecastsGenerated = 0;
        int goalsAnalysed = 0;
        int failures = 0;

        for (User user : users) {
            try {
                // Load the user's financial data once and share it across every phase.
                InsightContext context = contextFactory.build(user);
                insightsGenerated += insightService.generateForUser(user, context);
                // Health score is computed before the phases that consume it, so all stay consistent.
                healthScoreService.generateForUser(user, context);
                healthScoresGenerated++;
                recommendationsGenerated += recommendationService.generateForUser(user, context);
                forecastsGenerated += forecastService.generateForUser(user, context);
                goalsAnalysed += goalService.analyzeGoals(user, context);
                usersProcessed++;
            } catch (Exception ex) {
                failures++;
                log.error("[FinancialIntelligence] Failed processing user {}: {}",
                        user.getId(), ex.getMessage(), ex);
            }
        }

        long executionMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[FinancialIntelligence] Completed. usersProcessed={}, insightsGenerated={}, "
                        + "healthScoresGenerated={}, recommendationsGenerated={}, forecastsGenerated={}, "
                        + "goalsAnalysed={}, failures={}, executionMs={}",
                usersProcessed, insightsGenerated, healthScoresGenerated, recommendationsGenerated,
                forecastsGenerated, goalsAnalysed, failures, executionMs);
    }
}
