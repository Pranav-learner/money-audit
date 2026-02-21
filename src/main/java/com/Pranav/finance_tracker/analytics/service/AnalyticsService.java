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
        return savingRepository.getMonthlyTrend(user, year);
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

        String status = spentVal.compareTo(limit) > 0 ? "OVER_BUDGET" : "NORMAL";

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
        List<CategoryDistributionResponse> distribution = expenseRepository.getCategoryDistribution(user, start, end);

        String topCategory = (distribution != null && !distribution.isEmpty()) 
            ? distribution.get(0).getCategoryName() 
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
        
        return expenseRepository.getCategoryDistribution(user, start, end);
    }

    public List<SavingTrendItem> getSpendingTrend(User user, int year) {
        // SavingTrendItem can be reused for Spending trend (Month, Amount)
        return expenseRepository.getMonthlySpendingTrend(user, year);
    }

    public ExpenseTypeBreakdown getExpenseTypeBreakdown(User user, int month, int year) {
        BigDecimal direct = groupExpenseRepository.sumTotalDirectExpenseAsPayer(user.getId())
                .add(groupExpenseRepository.sumTotalDirectExpenseAsParticipant(user.getId()));
        
        BigDecimal group = groupExpenseRepository.sumTotalGroupExpenseAsPayer(user.getId())
                .add(groupExpenseRepository.sumTotalGroupExpenseAsParticipant(user.getId()));

        return new ExpenseTypeBreakdown(
                direct != null ? direct : BigDecimal.ZERO, 
                group != null ? group : BigDecimal.ZERO);
    }

    // ── Balance Analytics ──

    public BalanceOverviewResponse getBalanceOverview(User user) {
        BigDecimal owedByUser = splitRepository.sumTotalOwedByUser(user.getId());
        BigDecimal owedToUser = splitRepository.sumTotalOwedToUser(user.getId());
        
        BigDecimal paymentsSent = paymentRepository.sumTotalPaymentsSent(user.getId());
        BigDecimal paymentsReceived = paymentRepository.sumTotalPaymentsReceived(user.getId());

        BigDecimal netOwe = owedByUser.subtract(paymentsSent);
        BigDecimal netOwed = owedToUser.subtract(paymentsReceived);

        return BalanceOverviewResponse.builder()
                .youOwe(netOwe)
                .youAreOwed(netOwed)
                .netBalance(netOwed.subtract(netOwe))
                .build();
    }
}
