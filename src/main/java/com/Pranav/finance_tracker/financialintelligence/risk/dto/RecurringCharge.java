package com.Pranav.finance_tracker.financialintelligence.risk.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A recurring, subscription-like charge inferred from a user's expense history.
 *
 * <p>Produced by a {@link com.Pranav.finance_tracker.financialintelligence.risk.service.RecurringChargeDetector}
 * and consumed by both the subscription-risk and recurring-payment rules. It is a plain value
 * object so the detection strategy (heuristic today, ML tomorrow) can evolve without changing the
 * rules that read it.</p>
 */
@Getter
@Builder
public class RecurringCharge {

    /** Human-readable label the charge recurs under (e.g. an expense title or category). */
    private final String label;

    /** Business category the charge belongs to, or {@code null} if unknown. */
    private final String category;

    /** Representative (median) amount of a single occurrence. */
    private final BigDecimal typicalAmount;

    /** Number of individual expenses that make up this recurring charge. */
    private final int occurrences;

    /** Number of distinct calendar months the charge appeared in. */
    private final int monthsObserved;

    /** Typical day-of-month the charge lands on (1–31), or {@code null} if too irregular. */
    private final Integer typicalDayOfMonth;

    /** Date of the most recent occurrence. */
    private final LocalDate lastSeen;
}
