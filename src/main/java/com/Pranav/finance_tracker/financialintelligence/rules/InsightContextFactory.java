package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.savings.entity.Saving;
import com.Pranav.finance_tracker.savings.repository.SavingRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Assembles an {@link InsightContext} for a user by loading their financial data <b>once</b>.
 *
 * <p>Reuses existing repositories and {@link AnalyticsService} rather than duplicating SQL.
 * A single trailing-window expense query feeds the current-month, previous-month and
 * historical-baseline views, so the whole context costs only a handful of queries per user.</p>
 */
@Component
@RequiredArgsConstructor
public class InsightContextFactory {

    /** Number of trailing months (including the current one) loaded for baselines. */
    private static final int WINDOW_MONTHS = 6;

    private final ExpenseRepository expenseRepository;
    private final SavingRepository savingRepository;
    private final AnalyticsService analyticsService;

    /**
     * Builds a fully-populated context for the given user as of today.
     */
    public InsightContext build(User user) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDate windowStart = currentMonth.minusMonths(WINDOW_MONTHS - 1L).atDay(1);
        LocalDate windowEnd = currentMonth.atEndOfMonth();

        // One query covers the whole window; month buckets are derived in-memory.
        List<Expense> windowExpenses =
                expenseRepository.findByUserAndExpenseDateBetween(user, windowStart, windowEnd);

        List<Expense> currentMonthExpenses = windowExpenses.stream()
                .filter(e -> inMonth(e, currentMonth))
                .toList();
        List<Expense> previousMonthExpenses = windowExpenses.stream()
                .filter(e -> inMonth(e, previousMonth))
                .toList();

        List<BudgetUsageResponse> budgetUsages =
                analyticsService.getBudgetUsage(user, currentMonth.getMonthValue(), currentMonth.getYear());

        LocalDate lastSavingDate = savingRepository.findTop5ByUserOrderBySavingDateDesc(user).stream()
                .map(Saving::getSavingDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return InsightContext.builder()
                .user(user)
                .today(today)
                .currentMonth(currentMonth)
                .previousMonth(previousMonth)
                .currentMonthExpenses(currentMonthExpenses)
                .previousMonthExpenses(previousMonthExpenses)
                .windowExpenses(windowExpenses)
                .budgetUsages(budgetUsages)
                .lastSavingDate(lastSavingDate)
                .build();
    }

    private boolean inMonth(Expense expense, YearMonth month) {
        return expense.getExpenseDate() != null && YearMonth.from(expense.getExpenseDate()).equals(month);
    }
}
