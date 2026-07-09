package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.recommendation.dto.RecommendationSummaryResponse;
import com.Pranav.finance_tracker.financialintelligence.recommendation.service.RecommendationService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Exposes the user's top recommendations via the Personalized Recommendation Engine. */
@Component
@RequiredArgsConstructor
public class RecommendationTool implements FinancialTool {

    private final RecommendationService recommendationService;

    @Override
    public String key() {
        return ToolKeys.RECOMMENDATION;
    }

    @Override
    public String moduleLabel() {
        return "Recommendations";
    }

    @Override
    public ToolResult fetch(User user) {
        RecommendationSummaryResponse summary = recommendationService.getSummary(user);
        if (summary == null || summary.getActiveCount() == 0 || summary.getHighestPriority() == null) {
            return ToolResult.of(key(), moduleLabel(), "No active recommendations right now — nothing pressing to improve.");
        }
        BigDecimal monthly = summary.getPotentialMonthlySavings() == null ? BigDecimal.ZERO : summary.getPotentialMonthlySavings();
        String text = String.format(
                "You have %d active recommendation(s). The top one is \"%s\". Acting on them could save about %s per month.",
                summary.getActiveCount(), summary.getHighestPriority().getTitle(), MoneyFormatter.rupees(monthly));
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
