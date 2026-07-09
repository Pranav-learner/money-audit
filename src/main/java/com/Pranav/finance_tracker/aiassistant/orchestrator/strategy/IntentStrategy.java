package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;

import java.util.List;

/**
 * Strategy for recognising one {@link Intent} and declaring how to serve it.
 *
 * <p>Each intent is an independent strategy bean; the {@code IntentRouter} injects them all and
 * selects the highest-scoring one. Adding a new intent means adding a new strategy — no existing
 * code changes (Open/Closed, Strategy pattern). A future ML classifier could implement this same
 * interface to replace the keyword heuristics without affecting the router or orchestrator.</p>
 */
public interface IntentStrategy {

    /** The intent this strategy recognises. */
    Intent intent();

    /**
     * How strongly the message matches this intent (higher = better; 0 = no match).
     *
     * @param normalizedMessage the user message, lower-cased and trimmed
     */
    int score(String normalizedMessage);

    /** Tool keys to execute when this intent is selected. */
    List<String> toolKeys();

    /** Follow-up questions to suggest for this intent. */
    List<String> followUps();
}
