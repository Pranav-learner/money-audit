package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about the financial health score. */
@Component
public class FinancialHealthIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.FINANCIAL_HEALTH;
    }

    @Override
    protected List<String> keywords() {
        return List.of("financial health", "health score", "how am i doing", "financial score");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.HEALTH, ToolKeys.RISK);
    }

    @Override
    public List<String> followUps() {
        return List.of("Do you want recommendations to improve your Financial Health Score?",
                "Would you like to see your biggest financial risk?");
    }
}
