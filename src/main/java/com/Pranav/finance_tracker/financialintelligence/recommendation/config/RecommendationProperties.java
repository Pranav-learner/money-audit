package com.Pranav.finance_tracker.financialintelligence.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Externally-tunable knobs for the Personalized Recommendation Engine, bound from the
 * {@code recommendation.*} section of {@code application.yml}. Rules and the service read these via
 * constructor injection so behaviour can be re-tuned per deployment without code changes.
 */
@Component
@ConfigurationProperties(prefix = "recommendation")
@Data
public class RecommendationProperties {

    /** Recommendations with an expected monthly saving below this (₹) are suppressed as not worthwhile. */
    private BigDecimal minSavingsThreshold = new BigDecimal("100");

    /** Maximum number of active recommendations surfaced per user after prioritisation. */
    private int maxRecommendationsPerUser = 8;

    /** Recommendations below this confidence are dropped. */
    private double confidenceThreshold = 0.5;

    /** Minimum composite priority score required to keep a recommendation (0–100 scale). */
    private double priorityThreshold = 0.0;

    /** How many days a generated recommendation stays ACTIVE before expiring. */
    private int expirationDays = 30;

    /** Number of "top" recommendations returned by the /top endpoint and dashboard. */
    private int topCount = 3;

    /** Months of spending used as the emergency-fund target multiple. */
    private int emergencyFundMonths = 6;

    /** Target share of monthly inflow the savings rule nudges the user toward (e.g. 0.20 = 20%). */
    private double targetSavingsRate = 0.20;
}
