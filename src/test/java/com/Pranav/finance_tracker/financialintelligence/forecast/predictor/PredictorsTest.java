package com.Pranav.finance_tracker.financialintelligence.forecast.predictor;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.TestFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.ForecastFixtures;
import com.Pranav.finance_tracker.financialintelligence.forecast.dto.ForecastDraft;
import com.Pranav.finance_tracker.financialintelligence.forecast.entity.ForecastType;
import com.Pranav.finance_tracker.financialintelligence.forecast.predictor.impl.*;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PredictorsTest {

    /** 20th of a 31-day month, so month-end projection = value × 31 / 20. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);

    private InsightContext insightWith(String spent, String budget, String owed, int settlements) {
        List<Expense> current = spent == null ? List.of()
                : List.of(TestFixtures.expense(spent, TODAY, "Food"));
        List<BudgetUsageResponse> budgets = budget == null ? List.of()
                : List.of(TestFixtures.budgetUsage("All", budget, "0", budget, 0, "NORMAL"));
        return ForecastFixtures.insight(TODAY, current, budgets, owed, settlements);
    }

    @Test
    void monthlySpendingProjectsRunRate() {
        var ctx = ForecastFixtures.context(insightWith("8000", null, null, 0), "0", "0", 60);
        ForecastDraft draft = new MonthlySpendingPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.MONTHLY_SPENDING);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("12400"); // 8000 × 31 / 20
    }

    @Test
    void monthlySpendingSkipsWhenNothingSpent() {
        var ctx = ForecastFixtures.context(insightWith(null, null, null, 0), "0", "0", 60);
        assertThat(new MonthlySpendingPredictor().predict(ctx)).isNull();
    }

    @Test
    void monthlySavingsProjectsRunRate() {
        var ctx = ForecastFixtures.context(insightWith(null, null, null, 0), "0", "1000", 60);
        ForecastDraft draft = new MonthlySavingsPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.MONTHLY_SAVINGS);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("1550"); // 1000 × 31 / 20
    }

    @Test
    void budgetForecastFlagsProjectedOverrun() {
        var ctx = ForecastFixtures.context(insightWith("8000", "10000", null, 0), "0", "0", 60);
        ForecastDraft draft = new BudgetForecastPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.BUDGET_USAGE);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("124"); // 12400 / 10000
        assertThat(draft.getExplanation()).contains("exceeding");
    }

    @Test
    void budgetForecastSkipsWithoutBudget() {
        var ctx = ForecastFixtures.context(insightWith("8000", null, null, 0), "0", "0", 60);
        assertThat(new BudgetForecastPredictor().predict(ctx)).isNull();
    }

    @Test
    void cashFlowPredictsShortfall() {
        var ctx = ForecastFixtures.context(insightWith("8000", "10000", null, 0), "0", "0", 60);
        ForecastDraft draft = new CashFlowPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.CASHFLOW);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("-2400"); // 10000 − 12400
        assertThat(draft.getExplanation()).contains("shortfall");
    }

    @Test
    void debtForecastProjectsSteadyState() {
        var ctx = ForecastFixtures.context(insightWith(null, null, "5000", 2), "0", "0", 40);
        ForecastDraft draft = new DebtForecastPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.DEBT);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("5000");
    }

    @Test
    void debtForecastIsConfidentWhenDebtFree() {
        var ctx = ForecastFixtures.context(insightWith(null, null, null, 0), "0", "0", 90);
        ForecastDraft draft = new DebtForecastPredictor().predict(ctx);

        assertThat(draft.getPredictedValue()).isEqualByComparingTo("0");
        assertThat(draft.getConfidence()).isEqualTo(0.9);
    }

    @Test
    void netWorthProjectsForward() {
        // net worth = 20000 − 5000 = 15000; savings run-rate 1550/mo × 3 = 4650 → 19650.
        var ctx = ForecastFixtures.context(insightWith(null, null, "5000", 0), "20000", "1000", 70);
        ForecastDraft draft = new NetWorthPredictor().predict(ctx);

        assertThat(draft.getForecastType()).isEqualTo(ForecastType.NET_WORTH);
        assertThat(draft.getPredictedValue()).isEqualByComparingTo("19650");
    }
}
