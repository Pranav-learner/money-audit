package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;

import java.util.List;

/**
 * A single, self-contained piece of financial-analysis logic.
 *
 * <p>Every rule is its own Spring bean. The {@code InsightEngine} discovers all rules
 * automatically and runs each one against a per-user {@link InsightContext}, collecting the
 * drafts they emit. New rules can therefore be added by creating a new implementation —
 * no existing class needs to change (Open/Closed Principle).</p>
 */
public interface InsightRule {

    /**
     * Stable identifier of this rule, used for logging. Rules that emit multiple drafts
     * (e.g. one per category) may return more specific keys on the drafts themselves.
     */
    String ruleKey();

    /**
     * Evaluates the rule against the supplied context.
     *
     * @param context preloaded financial data for one user
     * @return zero or more insight drafts (never {@code null})
     */
    List<InsightDraft> evaluate(InsightContext context);
}
