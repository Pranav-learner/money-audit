package com.Pranav.finance_tracker.financialintelligence.risk.dto;

import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * An anomalous spending signal produced by a
 * {@link com.Pranav.finance_tracker.financialintelligence.risk.service.SpendingAnomalyDetector}.
 *
 * <p>Deliberately expressed in terms of primitive/business values (amount, label, score) rather
 * than an {@code Expense} entity, so the detector's public contract stays stable if the
 * implementation is later swapped for a machine-learning anomaly model.</p>
 */
@Getter
@Builder
public class SpendingAnomaly {

    /** Short description of what is anomalous (e.g. an expense title or "repeated charges"). */
    private final String label;

    /** Category the anomaly relates to, or {@code null}. */
    private final String category;

    /** The amount involved (single expense amount, or aggregate for repeated activity). */
    private final BigDecimal amount;

    /** Detector confidence in [0,1] that this is a genuine anomaly. */
    private final double confidence;

    /** Suggested severity, derived from how extreme the anomaly is. */
    private final Severity suggestedSeverity;

    /** Human-readable explanation, ready to drop into an insight description. */
    private final String explanation;

    /** Stable sub-key so multiple anomalies from one run de-duplicate independently. */
    private final String signature;
}
