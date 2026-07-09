package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about financial risk. */
@Component
public class RiskIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.RISK_ANALYSIS;
    }

    @Override
    protected List<String> keywords() {
        return List.of("risk", "risky", "danger", "worried", "biggest problem", "financial risk");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.RISK);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like recommendations to reduce this risk?",
                "Do you want to see your debt forecast?");
    }
}
