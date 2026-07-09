package com.Pranav.finance_tracker.aiassistant.orchestrator.strategy;

import com.Pranav.finance_tracker.aiassistant.orchestrator.Intent;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import org.springframework.stereotype.Component;

import java.util.List;

/** Recognises questions about the financial future / forecasts. */
@Component
public class ForecastIntentStrategy extends AbstractKeywordIntentStrategy {

    @Override
    public Intent intent() {
        return Intent.FORECAST;
    }

    @Override
    protected List<String> keywords() {
        return List.of("forecast", "predict", "future", "will happen", "if i continue", "month end", "end of month", "projection");
    }

    @Override
    public List<String> toolKeys() {
        return List.of(ToolKeys.FORECAST);
    }

    @Override
    public List<String> followUps() {
        return List.of("Would you like recommendations to change this trajectory?",
                "Do you want to see your projected month-end balance?");
    }
}
