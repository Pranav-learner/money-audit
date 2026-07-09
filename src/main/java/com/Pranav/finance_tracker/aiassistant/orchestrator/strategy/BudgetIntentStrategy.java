package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about budgets and limits. */
@Component
public class BudgetIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.BUDGET_ANALYSIS;
    }

    @Override
    protected List<String> keywords() {
        return List.of("budget", "over budget", "exceed", "limit", "overspend");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.BUDGET);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like to know why your Food budget increased?",
                "Do you want recommendations to stay within budget?");
    }
}
