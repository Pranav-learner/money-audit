package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about splits, settlements and who owes whom. */
@Component
public class SplitwiseIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.SPLITWISE;
    }

    @Override
    protected List<String> keywords() {
        return List.of("settle", "settlement", "who owes", "i owe", "split", "splitwise", "friend", "balance");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.SPLIT);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like to see your total outstanding balance?",
                "Do you want recommendations to clear your debts?");
    }
}
