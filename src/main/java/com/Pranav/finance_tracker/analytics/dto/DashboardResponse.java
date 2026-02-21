package com.Pranav.finance_tracker.analytics.dto;

import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalSpentMonth;
    private BigDecimal totalSavingsMonth;
    private BigDecimal netBalance;
    private List<BudgetUsageResponse> budgetAlerts; // Only those NEAR_LIMIT or OVER_BUDGET
    private List<CategoryDistributionResponse> categoryDistribution;
    private List<SavingTrendItem> monthlyTrend; // Reusing for spending trend
    private List<WeeklyTrendItem> weeklyTrend;
}
