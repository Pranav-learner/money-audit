package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about where money was spent. */
@Component
public class ExpenseIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.EXPENSE_SUMMARY;
    }

    @Override
    protected List<String> keywords() {
        return List.of("where did my money", "money go", "spend", "spent", "spending", "expense", "transaction", "cost");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.EXPENSE, ToolKeys.ANALYTICS);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like to see which category you spent the most on?",
                "Do you want your spending forecast for this month?");
    }
}
