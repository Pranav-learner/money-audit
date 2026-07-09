package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.analytics.dto.MonthlySavingsResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Exposes savings totals and month-over-month change via {@link AnalyticsService}. */
@Component
@RequiredArgsConstructor
public class SavingsTool implements FinancialTool {

    private final AnalyticsService analyticsService;

    @Override
    public String key() {
        return ToolKeys.SAVINGS;
    }

    @Override
    public String moduleLabel() {
        return "Savings";
    }

    @Override
    public ToolResult fetch(User user) {
        YearMonth current = YearMonth.from(LocalDate.now());
        YearMonth previous = current.minusMonths(1);

        MonthlySavingsResponse thisMonth = analyticsService.getMonthlySavings(user, current.getMonthValue(), current.getYear());
        MonthlySavingsResponse lastMonth = analyticsService.getMonthlySavings(user, previous.getMonthValue(), previous.getYear());
        BigDecimal total = analyticsService.getTotalSavings(user).getTotalSavings();

        BigDecimal now = thisMonth == null || thisMonth.getTotalSaved() == null ? BigDecimal.ZERO : thisMonth.getTotalSaved();
        BigDecimal prev = lastMonth == null || lastMonth.getTotalSaved() == null ? BigDecimal.ZERO : lastMonth.getTotalSaved();
        BigDecimal delta = now.subtract(prev);
        String direction = delta.signum() >= 0 ? "more" : "less";

        String text = String.format(
                "You've saved %s this month versus %s last month — %s %s. Total savings recorded: %s.",
                MoneyFormatter.rupees(now), MoneyFormatter.rupees(prev),
                MoneyFormatter.rupees(delta.abs()), direction,
                MoneyFormatter.rupees(total == null ? BigDecimal.ZERO : total));
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
