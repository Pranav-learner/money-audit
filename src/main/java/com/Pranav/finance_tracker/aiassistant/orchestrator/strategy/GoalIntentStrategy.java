package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about goals and affordability ("when can I buy…"). */
@Component
public class GoalIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.GOAL_PLANNING;
    }

    @Override
    protected List<String> keywords() {
        return List.of("goal", "when can i", "afford", "buy a", "buy new", "laptop", "vacation", "emergency fund", "target date");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.GOAL, ToolKeys.FORECAST);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like a plan to reach this goal sooner?",
                "Do you want to see the required monthly saving?");
    }
}
