package com.Pranav.finance_tracker.aiassistant.tools.impl;

import com.Pranav.finance_tracker.aiassistant.tools.FinancialTool;
import com.Pranav.finance_tracker.aiassistant.tools.ToolKeys;
import com.Pranav.finance_tracker.aiassistant.tools.ToolResult;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.financialintelligence.rules.MoneyFormatter;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Exposes category spending distribution (which category consumes the most) via {@link AnalyticsService}. */
@Component
@RequiredArgsConstructor
public class AnalyticsTool implements FinancialTool {

    private final AnalyticsService analyticsService;

    @Override
    public String key() {
        return ToolKeys.ANALYTICS;
    }

    @Override
    public String moduleLabel() {
        return "Analytics";
    }

    @Override
    public ToolResult fetch(User user) {
        LocalDate now = LocalDate.now();
        List<CategoryDistributionResponse> distribution =
                analyticsService.getCategoryDistribution(user, now.getMonthValue(), now.getYear(), "MONTH");
        if (distribution == null || distribution.isEmpty()) {
            return ToolResult.unavailable(key(), moduleLabel());
        }

        BigDecimal total = distribution.stream()
                .map(CategoryDistributionResponse::getValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CategoryDistributionResponse top = distribution.stream()
                .filter(c -> c.getValue() != null)
                .max(Comparator.comparing(CategoryDistributionResponse::getValue))
                .orElse(null);
        if (top == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return ToolResult.unavailable(key(), moduleLabel());
        }

        int share = top.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
        String text = String.format(
                "Your largest spending category this month is %s at %s, about %d%% of your %s total spending.",
                top.getName(), MoneyFormatter.rupees(top.getValue()), share, MoneyFormatter.rupees(total));
        return ToolResult.of(key(), moduleLabel(), text);
    }
}
