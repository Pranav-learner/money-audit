package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Exposes budget usage, highlighting the budget closest to its limit, via {@link AnalyticsService}. */
@Component
@RequiredArgsConstructor
public class BudgetTool implements FinancialTool {

    private final AnalyticsService analyticsService;

    @Override
    public String key() {
        return ToolKeys.BUDGET;
    }

    @Override
    public String moduleLabel() {
        return "Budgets";
    }

    @Override
    public ToolResult fetch(User user) {
        LocalDate now = LocalDate.now();
        List<BudgetUsageResponse> usages = analyticsService.getBudgetUsage(user, now.getMonthValue(), now.getYear());
        if (usages == null || usages.isEmpty()) {
            return ToolResult.unavailable(key(), moduleLabel());
        }

        BudgetUsageResponse closest = usages.stream()
                .max(Comparator.comparingInt(BudgetUsageResponse::getPercentageUsed))
                .orElseThrow();
        long overWarn = usages.stream().filter(u -> u.getPercentageUsed() >= 80).count();

        String text = String.format(
                "Your budget closest to its limit is %s at %d%% (%s spent of %s). %d budget(s) are at or above 80%% usage.",
                closest.getCategory(), closest.getPercentageUsed(),
                MoneyFormatter.rupees(closest.getSpent()), MoneyFormatter.rupees(closest.getBudget()), overWarn);
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
