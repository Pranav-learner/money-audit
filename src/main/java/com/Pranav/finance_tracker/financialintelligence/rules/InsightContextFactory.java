package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.analytics.dto.BalanceOverviewResponse;
import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.dto.SavingTrendItem;
import com.Pranav.finance_tracker.analytics.service.AnalyticsService;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.savings.entity.Saving;
import com.Pranav.finance_tracker.savings.repository.SavingRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
@Slf4j
public class InsightContextFactory {

    /** Number of trailing months (including the current one) loaded for baselines. */
    private static final int WINDOW_MONTHS = 6;

    private final ExpenseRepository expenseRepository;
    private final SavingRepository savingRepository;
    private final AnalyticsService analyticsService;
    private final GroupExpenseSplitRepository splitRepository;

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

        RiskData riskData = loadRiskData(user, today);

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
                .totalOwed(riskData.totalOwed())
                .totalOwedToUser(riskData.totalOwedToUser())
                .owedSettlementCount(riskData.owedSettlementCount())
                .savingsTrend(riskData.savingsTrend())
                .build();
    }

    /**
     * Loads the extra data risk rules need (debt + savings trend), reusing existing services and
     * repositories. Any failure here is downgraded to empty risk data so a hiccup in one subsystem
     * never blocks the whole insight run for a user.
     */
    private RiskData loadRiskData(User user, LocalDate today) {
        try {
            BalanceOverviewResponse balance = analyticsService.getBalanceOverview(user);
            BigDecimal totalOwed = balance != null ? balance.getYouOwe() : null;
            BigDecimal totalOwedToUser = balance != null ? balance.getYouAreOwed() : null;

            int owedSettlementCount = (int) splitRepository.findByUser(user).stream()
                    .filter(s -> !s.isSettled())
                    .filter(s -> s.getAmountOwed() != null && s.getAmountOwed().compareTo(BigDecimal.ZERO) > 0)
                    .count();

            List<SavingTrendItem> savingsTrend = analyticsService.getSavingsTrend(user, today.getYear());

            return new RiskData(totalOwed, totalOwedToUser, owedSettlementCount, savingsTrend);
        } catch (Exception ex) {
            log.warn("Could not load risk data for user {}: {}", user.getId(), ex.getMessage());
            return new RiskData(null, null, 0, List.of());
        }
    }

    /** Small carrier for the risk-specific slice of a user's context. */
    private record RiskData(BigDecimal totalOwed, BigDecimal totalOwedToUser,
                            int owedSettlementCount, List<SavingTrendItem> savingsTrend) {
    }

    private boolean inMonth(Expense expense, YearMonth month) {
        return expense.getExpenseDate() != null && YearMonth.from(expense.getExpenseDate()).equals(month);
    }
}
