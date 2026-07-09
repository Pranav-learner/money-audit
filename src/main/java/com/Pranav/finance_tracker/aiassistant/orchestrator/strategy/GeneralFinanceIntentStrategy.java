package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Broad "how are my finances" catch-all. It scores low so specific intents win, and the router also
 * falls back to it when nothing else matches.
 */
@Component
public class GeneralFinanceIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.GENERAL_FINANCE;
    }

    @Override
    protected List<String> keywords() {
        return List.of("finances", "financial", "money", "overview", "summary", "how are my");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.ANALYTICS, ToolKeys.HEALTH, ToolKeys.RECOMMENDATION);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like to see your biggest financial risk?",
                "Do you want recommendations to improve your finances?",
                "Would you like your spending forecast?");
    }
}
