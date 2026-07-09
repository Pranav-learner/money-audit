package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.analytics.dto.FinancialSummaryResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Exposes this month's spending totals via {@link AnalyticsService}. */
@Component
@RequiredArgsConstructor
public class ExpenseTool implements FinancialTool {

    private final AnalyticsService analyticsService;

    @Override
    public String key() {
        return ToolKeys.EXPENSE;
    }

    @Override
    public String moduleLabel() {
        return "Expenses";
    }

    @Override
    public ToolResult fetch(User user) {
        LocalDate now = LocalDate.now();
        FinancialSummaryResponse summary = analyticsService.getMonthlySummary(user, now.getMonthValue(), now.getYear());
        if (summary == null || summary.getTotalSpent() == null) {
            return ToolResult.unavailable(key(), moduleLabel());
        }
        String text = String.format(
                "This month you've spent %s. Your highest-spending category is %s. Net savings so far: %s.",
                MoneyFormatter.rupees(summary.getTotalSpent()),
                summary.getTopCategory() == null ? "n/a" : summary.getTopCategory(),
                MoneyFormatter.rupees(summary.getNetSavings()));
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
