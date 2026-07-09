package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.financialintelligence.dto.FinancialInsightResponse;
import com.Pranav.finance_tracker.financialintelligence.service.FinancialInsightService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Exposes the user's active financial risks via the Risk Detection Engine's insights. */
@Component
@RequiredArgsConstructor
public class RiskTool implements FinancialTool {

    private final FinancialInsightService insightService;

    @Override
    public String key() {
        return ToolKeys.RISK;
    }

    @Override
    public String moduleLabel() {
        return "Risk Detection";
    }

    @Override
    public ToolResult fetch(User user) {
        List<FinancialInsightResponse> risks = insightService.getActiveInsights(user).stream()
                .filter(i -> i.getRiskType() != null)
                .sorted(Comparator.comparing((FinancialInsightResponse i) -> i.getSeverity().ordinal()).reversed())
                .toList();
        if (risks.isEmpty()) {
            return ToolResult.of(key(), moduleLabel(), "No active financial risks were detected. You're in good standing.");
        }

        FinancialInsightResponse top = risks.get(0);
        String text = String.format("Your biggest current risk is \"%s\" (%s severity): %s. Total active risks: %d.",
                top.getTitle(), top.getSeverity(), top.getDescription(), risks.size());
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
