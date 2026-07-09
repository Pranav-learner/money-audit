package com.Pranav.finance_tracker.financialintelligence.risk.service;

import com.Pranav.finance_tracker.financialintelligence.risk.dto.RecurringCharge;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

import java.util.List;

/**
 * Detects recurring, subscription-like charges (Netflix, Spotify, gym, rent, EMI, …) from a user's
 * expense history <b>without hardcoding any merchant names</b>.
 *
 * <p>Defined as an interface so the detection strategy is pluggable: the shipped
 * {@link HeuristicRecurringChargeDetector} infers recurrence from repetition and cadence, and a
 * future ML-based detector can replace it by simply providing another bean — the subscription-risk
 * and recurring-payment rules depend only on this contract.</p>
 */
public interface RecurringChargeDetector {

    /**
     * Identifies recurring charges in the context's trailing expense window.
     *
     * @param context preloaded data for one user
     * @return detected recurring charges (never {@code null}; empty when none qualify)
     */
    List<RecurringCharge> detect(InsightContext context);
}
