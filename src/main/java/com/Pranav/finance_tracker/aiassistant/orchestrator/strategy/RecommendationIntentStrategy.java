package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises requests for recommendations / advice. */
@Component
public class RecommendationIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.RECOMMENDATION;
    }

    @Override
    protected List<String> keywords() {
        return List.of("recommend", "recommendation", "what should i", "improve", "advice", "suggest", "optimize", "optimise");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.RECOMMENDATION);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like to see the potential monthly savings?",
                "Do you want to act on the highest-priority recommendation?");
    }
}
