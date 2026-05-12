package com.Pranav.finance_tracker.analytics.controller;

import com.Pranav.finance_tracker.analytics.dto.*;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Visual insights into spending, savings, and budgets")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    // ── Savings Analytics ──

    @GetMapping("/savings/total")
    @Operation(summary = "Get total cumulative savings")
    public ResponseEntity<TotalSavingsResponse> getTotalSavings() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getTotalSavings(user));
    }

    @GetMapping("/savings/monthly")
    @Operation(summary = "Get savings for a specific month")
    public ResponseEntity<MonthlySavingsResponse> getMonthlySavings(
            @RequestParam int month,
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getMonthlySavings(user, month, year));
    }

    @GetMapping("/savings/trend")
    @Operation(summary = "Get yearly savings trend (monthly data)")
    public ResponseEntity<List<SavingTrendItem>> getSavingsTrend(
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getSavingsTrend(user, year));
    }

    // ── Budget Analytics ──

    @GetMapping("/budget/usage")
    @Operation(summary = "Get budget usage and status alerts")
    public ResponseEntity<List<BudgetUsageResponse>> getBudgetUsage(
            @RequestParam int month,
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getBudgetUsage(user, month, year));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Get high-level financial summary for a month")
    public ResponseEntity<FinancialSummaryResponse> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getMonthlySummary(user, month, year));
    }

    // ── Expense Analytics ──

    @GetMapping("/category-distribution")
    @Operation(summary = "Get spending breakdown by category (for pie charts)")
    public ResponseEntity<List<CategoryDistributionResponse>> getCategoryDistribution(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "MONTH") String period) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getCategoryDistribution(user, month, year, period));
    }

    @GetMapping("/spending-trend")
    @Operation(summary = "Get yearly spending trend")
    public ResponseEntity<List<SavingTrendItem>> getSpendingTrend(
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getSpendingTrend(user, year));
    }

    @GetMapping("/expense-type-breakdown")
    public ResponseEntity<ExpenseTypeBreakdown> getExpenseTypeBreakdown(
            @RequestParam int month,
            @RequestParam int year) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getExpenseTypeBreakdown(user, month, year));
    }

    // ── Balance Analytics ──

    @GetMapping("/balance-overview")
    @Operation(summary = "Get net balance (what you owe vs what you are owed)")
    public ResponseEntity<BalanceOverviewResponse> getBalanceOverview() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getBalanceOverview(user));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get all-in-one dashboard summary (multi-chart data)")
    public ResponseEntity<DashboardResponse> getDashboardSummary() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(analyticsService.getDashboardSummary(user));
    }
}
