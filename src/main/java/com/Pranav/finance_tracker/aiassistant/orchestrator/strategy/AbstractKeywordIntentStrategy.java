package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import java.util.List;

/**
 * Base {@link IntentStrategy} that scores a message by counting how many of its keywords/phrases
 * appear. Concrete strategies only declare their keywords, tools and follow-ups.
 */
public abstract class AbstractKeywordIntentStrategy implements IntentStrategy {

    /** Phrases that indicate this intent; multi-word phrases are matched as substrings. */
    protected abstract List<String> keywords();

    @Override
    public int score(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords()) {
            if (normalizedMessage.contains(keyword)) {
                // Longer, more specific phrases weigh more than single words.
                score += keyword.contains(" ") ? 2 : 1;
            }
        }
        return score;
    }
}
