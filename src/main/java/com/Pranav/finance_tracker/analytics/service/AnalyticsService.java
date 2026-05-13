package com.Pranav.finance_tracker.analytics.service;

import com.Pranav.finance_tracker.analytics.dto.*;
import com.Pranav.finance_tracker.budget.entity.Budget;
import com.Pranav.finance_tracker.budget.repository.BudgetRepository;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.savings.repository.SavingRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SavingRepository savingRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseSplitRepository splitRepository;
    private final PaymentRepository paymentRepository;

    // ── Savings Analytics ──

    public TotalSavingsResponse getTotalSavings(User user) {
        BigDecimal total = savingRepository.sumByUser(user);
        return new TotalSavingsResponse(total != null ? total : BigDecimal.ZERO);
    }

    public MonthlySavingsResponse getMonthlySavings(User user, int month, int year) {
        BigDecimal total = savingRepository.sumByUserAndMonthAndYear(user, month, year);
        return new MonthlySavingsResponse(month, year, total != null ? total : BigDecimal.ZERO);
    }

    public List<SavingTrendItem> getSavingsTrend(User user, int year) {
        List<Object[]> raw = savingRepository.getMonthlyTrend(user, year);
        return raw.stream()
                .map(obj -> new SavingTrendItem(((Number) obj[0]).intValue(), (BigDecimal) obj[1]))
                .collect(Collectors.toList());
    }

    // ── Budget Analytics ──

    public List<BudgetUsageResponse> getBudgetUsage(User user, int month, int year) {
        List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, month, year);
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();

        return budgets.stream().map(budget -> {
            BigDecimal spent = expenseRepository.sumByUserAndCategoryAndExpenseDateBetween(
                    user, budget.getCategory(), start, end);
            return calculateUsage(budget, spent);
        }).collect(Collectors.toList());
    }

    private BudgetUsageResponse calculateUsage(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal spentVal = spent != null ? spent : BigDecimal.ZERO;
        BigDecimal remaining = limit.subtract(spentVal);
        int percentage = (limit.compareTo(BigDecimal.ZERO) > 0)
                ? spentVal.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue()
                : 0;

        String status = "NORMAL";
        if (spentVal.compareTo(limit) > 0) {
            status = "OVER_BUDGET";
        } else if (percentage >= 80) {
            status = "NEAR_LIMIT";
        }

        return BudgetUsageResponse.builder()
                .category(budget.getCategory().getName())
                .budget(limit)
                .spent(spentVal)
                .remaining(remaining)
                .percentageUsed(percentage)
                .status(status)
                .build();
    }

    public FinancialSummaryResponse getMonthlySummary(User user, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();

        BigDecimal totalSavings = savingRepository.sumByUserAndMonthAndYear(user, month, year);
        BigDecimal totalSpent = expenseRepository.sumByUserAndExpenseDateBetween(user, start, end);
        List<CategoryDistributionResponse> distribution = getCategoryDistribution(user, month, year, "MONTH");

        String topCategory = (distribution != null && !distribution.isEmpty())
                ? distribution.get(0).getName()
                : "None";

        return FinancialSummaryResponse.builder()
                .totalIncomeSaved(totalSavings != null ? totalSavings : BigDecimal.ZERO)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .netSavings((totalSavings != null ? totalSavings : BigDecimal.ZERO)
                        .subtract(totalSpent != null ? totalSpent : BigDecimal.ZERO))
                .topCategory(topCategory)
                .build();
    }

    // ── Expense Analytics ──

    public List<CategoryDistributionResponse> getCategoryDistribution(User user, int month, int year, String period) {
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();
        LocalDate start;

        if ("WEEK".equalsIgnoreCase(period)) {
            start = end.minusDays(7);
        } else {
            start = LocalDate.of(year, month, 1);
        }

        List<CategoryDistributionResponse> list = expenseRepository.getCategoryDistribution(user, start, end);
        String[] colors = { "#6366f1", "#2dd4a8", "#fbbf24", "#f87171", "#a78bfa", "#f472b6" };

        for (int i = 0; i < list.size(); i++) {
            list.get(i).setColor(colors[i % colors.length]);
        }
        return list;
    }

    public List<Map<String, Object>> getSpendingTrend(User user, int year) {
        List<Object[]> raw = expenseRepository.getMonthlySpendingTrend(user, year);
        Map<Integer, BigDecimal> monthlySums = raw.stream()
                .collect(Collectors.toMap(
                        obj -> ((Number) obj[0]).intValue(),
                        obj -> (BigDecimal) obj[1]));

        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            trend.add(Map.of(
                    "month", months[i - 1],
                    "amount", monthlySums.getOrDefault(i, BigDecimal.ZERO)));
        }
        return trend;
    }

    public ExpenseTypeBreakdown getExpenseTypeBreakdown(User user, int month, int year) {
        BigDecimal pD = groupExpenseRepository.sumTotalDirectExpenseAsPayer(user.getId());
        BigDecimal partD = groupExpenseRepository.sumTotalDirectExpenseAsParticipant(user.getId());
        BigDecimal pG = groupExpenseRepository.sumTotalGroupExpenseAsPayer(user.getId());
        BigDecimal partG = groupExpenseRepository.sumTotalGroupExpenseAsParticipant(user.getId());

        BigDecimal direct = (pD != null ? pD : BigDecimal.ZERO).add(partD != null ? partD : BigDecimal.ZERO);
        BigDecimal group = (pG != null ? pG : BigDecimal.ZERO).add(partG != null ? partG : BigDecimal.ZERO);

        return new ExpenseTypeBreakdown(direct, group);
    }

    // ── Balance Analytics ──

    public BalanceOverviewResponse getBalanceOverview(User user) {
        BigDecimal owedByUser = splitRepository.sumTotalOwedByUser(user.getId());
        BigDecimal owedToUser = splitRepository.sumTotalOwedToUser(user.getId());

        BigDecimal paymentsSent = paymentRepository.sumTotalPaymentsSent(user.getId());
        BigDecimal paymentsReceived = paymentRepository.sumTotalPaymentsReceived(user.getId());

        BigDecimal personalExpenses = expenseRepository.sumAmountByUser(user);
        BigDecimal totalSavings = savingRepository.sumByUser(user);

        // Sum of all expenses where I was the payer (Initial cash outflow for splits)
        BigDecimal pD = groupExpenseRepository.sumTotalDirectExpenseAsPayer(user.getId());
        BigDecimal pG = groupExpenseRepository.sumTotalGroupExpenseAsPayer(user.getId());
        BigDecimal totalOutlay = (pD != null ? pD : BigDecimal.ZERO).add(pG != null ? pG : BigDecimal.ZERO);

        BigDecimal personalVal = personalExpenses != null ? personalExpenses : BigDecimal.ZERO;
        BigDecimal savingsVal = totalSavings != null ? totalSavings : BigDecimal.ZERO;
        BigDecimal sentVal = paymentsSent != null ? paymentsSent : BigDecimal.ZERO;
        BigDecimal receivedVal = paymentsReceived != null ? paymentsReceived : BigDecimal.ZERO;

        // Debt (Unpaid)
        BigDecimal owedVal = owedByUser != null ? owedByUser : BigDecimal.ZERO;
        BigDecimal toOweVal = owedToUser != null ? owedToUser : BigDecimal.ZERO;
        BigDecimal netOwe = owedVal.subtract(sentVal);
        BigDecimal netOwed = toOweVal.subtract(receivedVal);

        // Net Balance = (Actual Cash In) - (Actual Cash Out)
        // In: Savings + Received Settlements
        // Out: Personal Expenses + Settlements I Paid + Total Outlay I Paid for others
        // up front
        BigDecimal netBalance = savingsVal.add(receivedVal)
                .subtract(personalVal)
                .subtract(sentVal)
                .subtract(totalOutlay);

        return BalanceOverviewResponse.builder()
                .youOwe(netOwe)
                .youAreOwed(netOwed)
                .netBalance(netBalance)
                .build();
    }

    public DashboardResponse getDashboardSummary(User user) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalSavings = savingRepository.sumByUserAndMonthAndYear(user, month, year);
        BigDecimal totalSpent = expenseRepository.sumByUserAndExpenseDateBetween(user, start, end);

        List<BudgetUsageResponse> budgetUsage = getBudgetUsage(user, month, year);
        List<BudgetUsageResponse> alerts = budgetUsage.stream()
                .filter(b -> !"NORMAL".equals(b.getStatus()))
                .toList();

        List<CategoryDistributionResponse> distribution = getCategoryDistribution(user, month, year, "MONTH");
        List<Map<String, Object>> trend = getSpendingTrend(user, year);

        BalanceOverviewResponse balance = getBalanceOverview(user);
        List<WeeklyTrendItem> weeklyTrend = getWeeklyTrend(user);

        // Total Budget logic
        BigDecimal totalBudget = budgetUsage.stream()
                .map(BudgetUsageResponse::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBudgetSpent = budgetUsage.stream()
                .map(BudgetUsageResponse::getSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Enforce: Total budget can never exceed the saving/Income
        BigDecimal savingsVal = totalSavings != null ? totalSavings : BigDecimal.ZERO;
        if (totalBudget.compareTo(savingsVal) > 0) {
            totalBudget = savingsVal;
        }

        int budgetRemainingPct = 0;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = totalBudget.subtract(totalBudgetSpent);
            if (remaining.compareTo(BigDecimal.ZERO) < 0)
                remaining = BigDecimal.ZERO;
            budgetRemainingPct = remaining.multiply(BigDecimal.valueOf(100))
                    .divide(totalBudget, 0, RoundingMode.HALF_UP).intValue();
        }

        // Previous Month Data
        LocalDate prevMonthDate = now.minusMonths(1);
        int pMonth = prevMonthDate.getMonthValue();
        int pYear = prevMonthDate.getYear();
        LocalDate pStart = prevMonthDate.withDayOfMonth(1);
        LocalDate pEnd = prevMonthDate.withDayOfMonth(prevMonthDate.lengthOfMonth());

        BigDecimal prevSavings = savingRepository.sumByUserAndMonthAndYear(user, pMonth, pYear);
        BigDecimal prevSpent = expenseRepository.sumByUserAndExpenseDateBetween(user, pStart, pEnd);

        BigDecimal expenseRate = BigDecimal.ZERO;
        BigDecimal spentVal = totalSpent != null ? totalSpent : BigDecimal.ZERO;
        if (savingsVal.compareTo(BigDecimal.ZERO) > 0) {
            expenseRate = spentVal.multiply(BigDecimal.valueOf(100))
                    .divide(savingsVal, 1, RoundingMode.HALF_UP);
        }

        return DashboardResponse.builder()
                .totalSpentMonth(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .totalSavingsMonth(totalSavings != null ? totalSavings : BigDecimal.ZERO)
                .netBalance(balance.getNetBalance())
                .expenseTrend(calculateGrowth(totalSpent, prevSpent))
                .savingsTrend(calculateGrowth(totalSavings, prevSavings))
                .balanceTrend(expenseRate + "%")
                .budgetAlerts(alerts)
                .categoryDistribution(distribution)
                .monthlyTrend(trend)
                .weeklyTrend(weeklyTrend)
                .totalBudget(totalBudget)
                .totalBudgetSpent(totalBudgetSpent)
                .budgetRemainingPct(budgetRemainingPct)
                .build();
    }

    private String calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? "+100%" : "0%";
        }
        BigDecimal curr = current != null ? current : BigDecimal.ZERO;
        BigDecimal growth = curr.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);

        return (growth.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + growth + "%";
    }

    private List<WeeklyTrendItem> getWeeklyTrend(User user) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusWeeks(8);

        List<com.Pranav.finance_tracker.expense.entity.Expense> expenses = expenseRepository
                .findByUserAndExpenseDateBetween(user, start, end);

        // Group by week (using start of week as key)
        Map<LocalDate, BigDecimal> weeklySums = expenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getExpenseDate()
                                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                        Collectors.mapping(
                                com.Pranav.finance_tracker.expense.entity.Expense::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        // Ensure all 8 weeks are present even if zero
        List<WeeklyTrendItem> trend = new ArrayList<>();
        LocalDate currentWeek = start
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        while (!currentWeek.isAfter(end)) {
            trend.add(new WeeklyTrendItem(currentWeek, weeklySums.getOrDefault(currentWeek, BigDecimal.ZERO)));
            currentWeek = currentWeek.plusWeeks(1);
        }

        return trend;
    }
}
