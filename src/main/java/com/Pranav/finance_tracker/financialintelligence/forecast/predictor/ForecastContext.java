package com.Pranav.finance_tracker.financialintelligence.forecast.predictor;

import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Immutable per-user data set for one forecasting run.
 *
 * <p>Composes the shared, preloaded {@link InsightContext} (spending, budgets, debt, savings trend —
 * <b>reused, not re-queried</b>) with the extra signals predictors need: the user's total savings,
 * this month's savings and their health score. Common month-projection maths lives here so the six
 * predictors stay focused on their specific prediction and never duplicate the run-rate formula.</p>
 */
@Getter
@Builder
public class ForecastContext {

    private final InsightContext insight;
    private final BigDecimal totalSavings;
    private final BigDecimal savedThisMonth;
    private final int healthScore;

    // ── Shared projection helpers ───────────────────────────────────────

    public LocalDate today() {
        return insight.getToday();
    }

    public YearMonth currentMonth() {
        return insight.getCurrentMonth();
    }

    public int daysElapsed() {
        return today().getDayOfMonth();
    }

    public int daysInMonth() {
        return currentMonth().lengthOfMonth();
    }

    public LocalDate monthEnd() {
        return currentMonth().atEndOfMonth();
    }

    /** Human-readable period label, e.g. {@code 2026-07}. */
    public String periodLabel() {
        return currentMonth().toString();
    }

    /** Fraction of the month elapsed, in (0, 1]. */
    public double monthProgress() {
        return (double) daysElapsed() / daysInMonth();
    }

    /** Total spend so far this month. */
    public BigDecimal spentSoFar() {
        return insight.totalSpend(insight.getCurrentMonthExpenses());
    }

    /** Straight-line projection of month-end spend from the current run-rate. */
    public BigDecimal projectedMonthEndSpend() {
        int elapsed = Math.max(1, daysElapsed());
        return spentSoFar()
                .multiply(BigDecimal.valueOf(daysInMonth()))
                .divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
    }

    /** Straight-line projection of month-end savings from this month's run-rate. */
    public BigDecimal projectedMonthEndSavings() {
        BigDecimal saved = savedThisMonth == null ? BigDecimal.ZERO : savedThisMonth;
        int elapsed = Math.max(1, daysElapsed());
        return saved
                .multiply(BigDecimal.valueOf(daysInMonth()))
                .divide(BigDecimal.valueOf(elapsed), 2, RoundingMode.HALF_UP);
    }

    /** Net worth = total savings − total outstanding debt. */
    public BigDecimal netWorth() {
        BigDecimal savings = totalSavings == null ? BigDecimal.ZERO : totalSavings;
        BigDecimal owed = insight.getTotalOwed() == null ? BigDecimal.ZERO : insight.getTotalOwed();
        return savings.subtract(owed);
    }

    /** Confidence that grows with how much of the month has elapsed, capped for safety. */
    public double progressConfidence() {
        return Math.min(0.95, 0.5 + monthProgress() * 0.4);
    }
}
