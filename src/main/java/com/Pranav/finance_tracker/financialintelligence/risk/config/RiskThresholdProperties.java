package com.Pranav.finance_tracker.financialintelligence.risk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Externally-tunable thresholds for the Risk Detection Engine.
 *
 * <p>Bound from the {@code risk.*} section of {@code application.yml}. Every risk rule reads its
 * limits from here via constructor injection, so a deployment can re-tune sensitivity (debt limit,
 * budget %, savings gap, subscription cost, …) purely through configuration — no code change,
 * no redeploy of new logic. Sensible defaults are provided so the engine works out-of-the-box.</p>
 */
@Component
@ConfigurationProperties(prefix = "risk")
@Data
public class RiskThresholdProperties {

    /** Absolute outstanding-debt amount (₹) at/above which a debt risk is raised. */
    private BigDecimal debtThreshold = new BigDecimal("10000");

    /** Number of unsettled settlements at/above which a debt risk is raised regardless of amount. */
    private int overdueSettlementCount = 3;

    /** Budget usage % at/above which a budget risk becomes MEDIUM severity. */
    private int budgetWarnPercent = 80;

    /** Budget usage % at/above which a budget risk becomes HIGH severity. */
    private int budgetOverPercent = 100;

    /** Minimum days into the month before cash-flow projection is considered trustworthy. */
    private int cashflowMinElapsedDays = 5;

    /** Fraction (0–1) by which projected spend must exceed the budget ceiling to raise a HIGH risk. */
    private double cashflowOverrunBuffer = 0.0;

    /** Percentage drop in recent savings that constitutes a declining-savings risk. */
    private int savingsDeclinePercent = 25;

    /** Days without a savings contribution that constitutes a savings gap. */
    private int savingsGapDays = 45;

    /** Minimum distinct months a charge must recur in before it counts as a subscription. */
    private int subscriptionMinMonths = 3;

    /** Monthly cost (₹) at/above which a recurring subscription is flagged as expensive. */
    private BigDecimal subscriptionCostThreshold = new BigDecimal("500");

    /** Multiple of the recent weekly average that current-week spend must exceed to be a spike. */
    private double spikeMultiplier = 1.75;

    /** Absolute floor (₹) below which a weekly spike is ignored as noise. */
    private BigDecimal spikeMinWeeklyAmount = new BigDecimal("1000");

    /** Days ahead of an expected recurring-payment date within which a reminder is generated. */
    private int recurringDueWindowDays = 5;

    /** Z-score at/above which a transaction is treated as a statistical anomaly. */
    private double anomalyZScore = 2.5;

    /** Z-score at/above which an anomaly is escalated to HIGH severity. */
    private double anomalyHighZScore = 3.5;

    /** Count of identical-amount transactions in the current month that flags repeated activity. */
    private int repeatedTransactionCount = 4;
}
