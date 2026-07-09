package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.analytics.dto.BalanceOverviewResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Exposes Splitwise-style balances (what you owe / are owed) via {@link AnalyticsService}. */
@Component
@RequiredArgsConstructor
public class SplitTool implements FinancialTool {

    private final AnalyticsService analyticsService;

    @Override
    public String key() {
        return ToolKeys.SPLIT;
    }

    @Override
    public String moduleLabel() {
        return "Splitwise";
    }

    @Override
    public ToolResult fetch(User user) {
        BalanceOverviewResponse balance = analyticsService.getBalanceOverview(user);
        if (balance == null) {
            return ToolResult.unavailable(key(), moduleLabel());
        }
        BigDecimal owe = balance.getYouOwe() == null ? BigDecimal.ZERO : balance.getYouOwe();
        BigDecimal owed = balance.getYouAreOwed() == null ? BigDecimal.ZERO : balance.getYouAreOwed();

        String text = String.format(
                "You currently owe %s and are owed %s (net %s). Prioritise settling your largest outstanding balance first.",
                MoneyFormatter.rupees(owe), MoneyFormatter.rupees(owed),
                MoneyFormatter.rupees(balance.getNetBalance() == null ? owed.subtract(owe) : balance.getNetBalance()));
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
