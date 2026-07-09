package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about savings. */
@Component
public class SavingsIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.SAVINGS_ANALYSIS;
    }

    @Override
    protected List<String> keywords() {
        return List.of("saving", "savings", "saved", "save more", "set aside");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.SAVINGS, ToolKeys.ANALYTICS);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like tips to increase your monthly savings?",
                "Do you want to set a savings goal?");
    }
}
