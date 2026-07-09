package com.Pranav.finance_tracker.aiassistant.orchestrator;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * The outcome of intent routing: the detected {@link Intent}, the tool keys to execute and the
 * follow-up questions to suggest.
 */
@Getter
@Builder
public class RoutingResult {

    private final Intent intent;
    private final List<String> toolKeys;
    private final List<String> followUps;

    /** Router confidence in the classification, in [0.0, 1.0]. */
    private final double confidence;
}
