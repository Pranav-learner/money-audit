package com.Pranav.finance_tracker.financialintelligence.healthscore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externally-tunable configuration for the Financial Health Score Engine, bound from
 * {@code healthscore.*} in {@code application.yml}. The component weights must sum to 100; each
 * calculator reads its own weight, so re-weighting the score is a config change, not a code change.
 */
@Component
@ConfigurationProperties(prefix = "healthscore")
@Data
public class HealthScoreProperties {

    /** Maximum points contributed by budget adherence. */
    private int budgetWeight = 30;

    /** Maximum points contributed by savings behaviour. */
    private int savingsWeight = 30;

    /** Maximum points contributed by debt management. */
    private int debtWeight = 25;

    /** Maximum points contributed by spending stability. */
    private int spendingWeight = 15;

    /** Days of score history retained per user before older rows may be pruned. */
    private int historyRetentionDays = 365;

    /** A drop of at least this many points versus the previous score triggers a notification. */
    private int dropNotificationPoints = 10;
}
