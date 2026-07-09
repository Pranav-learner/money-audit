package com.Pranav.finance_tracker.financialintelligence.forecast;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.ForecastContext;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Shared builders for forecasting/planning unit tests.
 */
public final class ForecastFixtures {

    private ForecastFixtures() {
    }

    public static InsightContext insight(LocalDate today, List<Expense> currentMonthExpenses,
                                         List<BudgetUsageResponse> budgets, String totalOwed, int settlements) {
        return InsightContext.builder()
                .user(TestFixtures.user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(currentMonthExpenses)
                .previousMonthExpenses(List.of())
                .windowExpenses(currentMonthExpenses)
                .budgetUsages(budgets)
                .totalOwed(totalOwed == null ? null : new BigDecimal(totalOwed))
                .owedSettlementCount(settlements)
                .build();
    }

    public static ForecastContext context(InsightContext insight, String totalSavings,
                                          String savedThisMonth, int healthScore) {
        return ForecastContext.builder()
                .insight(insight)
                .totalSavings(new BigDecimal(totalSavings))
                .savedThisMonth(new BigDecimal(savedThisMonth))
                .healthScore(healthScore)
                .build();
    }
}
