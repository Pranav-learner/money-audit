package com.Pranav.finance_tracker.financialintelligence.rules;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.dto.SavingTrendItem;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of one user's financial data for a single analysis run.
 *
 * <p>Built once per user by {@link InsightContextFactory} so that every rule reads from the
 * same in-memory data set — this avoids N+1 queries and keeps rules pure (no repository access).
 * Convenience aggregation helpers live here so rules stay focused on decision logic.</p>
 */
@Getter
@Builder
public class InsightContext {

    private final User user;
    private final LocalDate today;
    private final YearMonth currentMonth;
    private final YearMonth previousMonth;

    /** Expenses in the current calendar month. */
    private final List<Expense> currentMonthExpenses;

    /** Expenses in the previous calendar month. */
    private final List<Expense> previousMonthExpenses;

    /** A wider trailing window (several months) used for historical baselines. */
    private final List<Expense> windowExpenses;

    /** Budget usage for the current month, reused from the existing AnalyticsService. */
    private final List<BudgetUsageResponse> budgetUsages;

    /** Date of the user's most recent savings entry, or {@code null} if they have none. */
    private final LocalDate lastSavingDate;

    // ── Risk-detection data (Module 2) ──────────────────────────────────
    // Populated once per user alongside the spending data above so risk rules incur no extra
    // per-rule queries. All fields are optional: Module 1 rules and tests ignore them, and risk
    // rules null-check before use.

    /** Total amount the user currently owes across unsettled splits/settlements (₹), or {@code null}. */
    private final BigDecimal totalOwed;

    /** Total amount other users currently owe this user (₹), or {@code null}. */
    private final BigDecimal totalOwedToUser;

    /** Number of distinct unsettled settlements the user owes into. */
    private final int owedSettlementCount;

    /** Monthly savings totals for the current year (from AnalyticsService), or {@code null}. */
    private final List<SavingTrendItem> savingsTrend;

    // ── Aggregation helpers ─────────────────────────────────────────────

    /** Sum of the given expenses, treating null amounts as zero. */
    public BigDecimal totalSpend(List<Expense> expenses) {
        if (expenses == null) {
            return BigDecimal.ZERO;
        }
        return expenses.stream()
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum of the given expenses restricted to a single category (case-insensitive name match). */
    public BigDecimal spendForCategory(List<Expense> expenses, String categoryName) {
        if (expenses == null || categoryName == null) {
            return BigDecimal.ZERO;
        }
        return expenses.stream()
                .filter(e -> e.getCategory() != null && categoryName.equalsIgnoreCase(e.getCategory().getName()))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total spend per category name for the given expenses. */
    public Map<String, BigDecimal> categoryTotals(List<Expense> expenses) {
        if (expenses == null) {
            return Collections.emptyMap();
        }
        return expenses.stream()
                .filter(e -> e.getCategory() != null && e.getCategory().getName() != null && e.getAmount() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    /**
     * Average value of a single expense across the trailing window — the user's "typical" spend.
     * Returns {@link Optional#empty()} when there is not enough history to be meaningful.
     */
    public Optional<BigDecimal> historicalAverageExpenseAmount(int minimumSample) {
        List<BigDecimal> amounts = windowExpenses == null ? List.of()
                : windowExpenses.stream().map(Expense::getAmount).filter(Objects::nonNull).toList();
        if (amounts.size() < minimumSample) {
            return Optional.empty();
        }
        BigDecimal sum = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(amounts.size()), 2, java.math.RoundingMode.HALF_UP));
    }

    /**
     * Days since the user last recorded savings. When they have never saved, this falls back to
     * the account age so brand-new users are not nagged prematurely.
     */
    public long daysSinceLastSaving() {
        LocalDate reference = lastSavingDate;
        if (reference == null) {
            reference = (user != null && user.getCreatedAt() != null)
                    ? user.getCreatedAt().toLocalDate()
                    : today;
        }
        return ChronoUnit.DAYS.between(reference, today);
    }

    /** Sum of all category budget limits for the current month, or {@link BigDecimal#ZERO} if none. */
    public BigDecimal totalBudget() {
        if (budgetUsages == null) {
            return BigDecimal.ZERO;
        }
        return budgetUsages.stream()
                .map(BudgetUsageResponse::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total spend within an inclusive date range, drawn from the trailing window.
     * Used by short-horizon rules (e.g. weekly spike detection).
     */
    public BigDecimal spendBetween(LocalDate startInclusive, LocalDate endInclusive) {
        if (windowExpenses == null || startInclusive == null || endInclusive == null) {
            return BigDecimal.ZERO;
        }
        return windowExpenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .filter(e -> !e.getExpenseDate().isBefore(startInclusive) && !e.getExpenseDate().isAfter(endInclusive))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
