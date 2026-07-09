package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.HealthScoreProvider;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContextFactory;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Exposes the user's financial health score via the Health Score Engine (the {@link HealthScoreProvider} seam). */
@Component
@RequiredArgsConstructor
public class HealthScoreTool implements FinancialTool {

    private final HealthScoreProvider healthScoreProvider;
    private final InsightContextFactory insightContextFactory;

    @Override
    public String key() {
        return ToolKeys.HEALTH;
    }

    @Override
    public String moduleLabel() {
        return "Financial Health";
    }

    @Override
    public ToolResult fetch(User user) {
        int score = healthScoreProvider.scoreFor(insightContextFactory.build(user));
        String band = score >= 80 ? "excellent" : score >= 60 ? "good" : score >= 40 ? "fair" : "needs attention";
        String text = String.format(
                "Your financial health score is %d/100 (%s). It reflects your budget adherence, savings habit and debt load.",
                score, band);
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
