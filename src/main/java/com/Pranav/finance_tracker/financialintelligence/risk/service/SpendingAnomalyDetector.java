package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.financialintelligence.risk.dto.SpendingAnomaly;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

import java.util.List;

/**
 * Detects unusual spending activity for a user.
 *
 * <p>This is the seam designed for future machine-learning anomaly detection: the shipped
 * {@link StatisticalAnomalyDetector} uses a simple z-score / repeated-transaction heuristic, but a
 * model-backed implementation can be substituted as a Spring bean without changing the
 * {@code UnusualActivityRule}, its public API, DTOs or the database schema.</p>
 */
public interface SpendingAnomalyDetector {

    /**
     * Scores the user's recent activity and returns any anomalies found.
     *
     * @param context preloaded data for one user
     * @return detected anomalies (never {@code null}; empty when nothing is unusual)
     */
    List<SpendingAnomaly> detect(InsightContext context);
}
