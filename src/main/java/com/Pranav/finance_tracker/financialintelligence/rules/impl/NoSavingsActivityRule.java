package com.Pranav.finance_tracker.financialintelligence.rules.impl;

import com.Pranav.finance_tracker.financialintelligence.dto.InsightDraft;
import com.Pranav.finance_tracker.financialintelligence.entity.InsightType;
import com.Pranav.finance_tracker.financialintelligence.entity.Severity;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rule 6 — reminds the user when they have not recorded savings for over 30 days.
 *
 * <p>For users who have never saved, the account age is used as the reference point (via the
 * context helper), so newly-registered users are not nagged in their first month.</p>
 */
@Component
public class NoSavingsActivityRule implements InsightRule {

    private static final String RULE_KEY = "NO_SAVINGS_ACTIVITY";
    private static final long INACTIVITY_THRESHOLD_DAYS = 30;

    @Override
    public String ruleKey() {
        return RULE_KEY;
    }

    @Override
    public List<InsightDraft> evaluate(InsightContext context) {
        long days = context.daysSinceLastSaving();
        if (days <= INACTIVITY_THRESHOLD_DAYS) {
            return List.of();
        }

        boolean neverSaved = context.getLastSavingDate() == null;
        String description = neverSaved
                ? "You haven't recorded any savings yet. Even a small amount adds up over time."
                : String.format("You haven't recorded any savings in %d days.", days);

        InsightDraft draft = InsightDraft.builder()
                .ruleKey(RULE_KEY)
                .title("Time to top up your savings")
                .description(description)
                .insightType(InsightType.INFORMATION)
                .severity(Severity.MEDIUM)
                .category(null)
                .actionSuggestion("Set aside a small amount this week to keep your savings habit going.")
                .confidence(0.9)
                .build();

        return List.of(draft);
    }
}
