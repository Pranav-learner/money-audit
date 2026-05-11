package com.Pranav.finance_tracker.dashboard.controller;

import com.Pranav.finance_tracker.analytics.dto.DashboardResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.savings.entity.Saving;
import com.Pranav.finance_tracker.savings.repository.SavingRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final ExpenseRepository expenseRepository;
    private final SavingRepository savingRepository;
    private final SecurityUtils securityUtils;

    public record DashboardSummaryResponse(
            BigDecimal totalBalance,
            BigDecimal monthlyExpenses,
            BigDecimal totalSavings,
            int budgetUsedPct,
            Object trends
    ) {}

    public record DashboardChartsResponse(
            Object spendingOverview,
            Object categoryDistribution,
            Object expenseTrend
    ) {}

    public record RecentActivityResponse(
            String id,
            String title,
            BigDecimal amount,
            LocalDate date,
            String type,
            String icon
    ) {}

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        User user = securityUtils.getCurrentUser();
        DashboardResponse analytics = analyticsService.getDashboardSummary(user);

        // Calculate a rough average budget usage for the dashboard top level
        int totalBudgetPct = 0;
        if (analytics.getBudgetAlerts() != null && !analytics.getBudgetAlerts().isEmpty()) {
             totalBudgetPct = analytics.getBudgetAlerts().stream()
                     .mapToInt(b -> b.getPercentageUsed())
                     .max().orElse(0); // Return highest usage or could calculate average
        }

        DashboardSummaryResponse response = new DashboardSummaryResponse(
                analytics.getNetBalance(),
                analytics.getTotalSpentMonth(),
                analytics.getTotalSavingsMonth(),
                totalBudgetPct,
                Map.of("monthlyTrend", analytics.getMonthlyTrend())
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/charts")
    public ResponseEntity<DashboardChartsResponse> getCharts() {
        User user = securityUtils.getCurrentUser();
        DashboardResponse analytics = analyticsService.getDashboardSummary(user);

        DashboardChartsResponse response = new DashboardChartsResponse(
                analytics.getWeeklyTrend(),
                analytics.getCategoryDistribution(),
                analytics.getMonthlyTrend()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<RecentActivityResponse>> getRecentActivity() {
        User user = securityUtils.getCurrentUser();
        
        List<Expense> recentExpenses = expenseRepository.findTop5ByUserOrderByExpenseDateDesc(user);
        List<Saving> recentSavings = savingRepository.findTop5ByUserOrderBySavingDateDesc(user);

        List<RecentActivityResponse> activities = new ArrayList<>();

        for (Expense e : recentExpenses) {
            activities.add(new RecentActivityResponse(
                    e.getId().toString(),
                    e.getTitle() != null ? e.getTitle() : "Expense",
                    e.getAmount(),
                    e.getExpenseDate(),
                    "expense",
                    e.getCategory() != null ? e.getCategory().getIcon() : "tag"
            ));
        }

        for (Saving s : recentSavings) {
            activities.add(new RecentActivityResponse(
                    s.getId().toString(),
                    s.getTitle() != null ? s.getTitle() : "Saving",
                    s.getAmount(),
                    s.getSavingDate(),
                    "saving",
                    "savings"
            ));
        }

        // Sort both by date descending, take top 5 overall
        activities.sort(Comparator.comparing(RecentActivityResponse::date).reversed());
        if (activities.size() > 5) {
            activities = activities.subList(0, 5);
        }

        return ResponseEntity.ok(activities);
    }
}
