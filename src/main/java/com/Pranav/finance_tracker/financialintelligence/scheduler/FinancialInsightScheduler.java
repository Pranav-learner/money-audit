package com.Pranav.finance_tracker.financialintelligence.scheduler;

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
 * <p>Runs at 02:00 every day, iterates over all users and asks the service to generate their
 * insights. Each user is processed in its own transaction (inside the service), so a failure
 * for one user is logged and isolated without aborting the whole run.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinancialInsightScheduler {

    private final UserRepository userRepository;
    private final FinancialInsightService insightService;

    /** Every night at 2 AM: {@code second minute hour day-of-month month day-of-week}. */
    @Scheduled(cron = "0 0 2 * * *")
    public void generateNightlyInsights() {
        long startNanos = System.nanoTime();
        List<User> users = userRepository.findAll();
        log.info("[FinancialIntelligence] Nightly insight generation started for {} user(s)", users.size());

        int usersProcessed = 0;
        int insightsGenerated = 0;
        int failures = 0;

        for (User user : users) {
            try {
                insightsGenerated += insightService.generateForUser(user);
                usersProcessed++;
            } catch (Exception ex) {
                failures++;
                log.error("[FinancialIntelligence] Failed generating insights for user {}: {}",
                        user.getId(), ex.getMessage(), ex);
            }
        }

        long executionMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[FinancialIntelligence] Completed. usersProcessed={}, insightsGenerated={}, failures={}, executionMs={}",
                usersProcessed, insightsGenerated, failures, executionMs);
    }
}
