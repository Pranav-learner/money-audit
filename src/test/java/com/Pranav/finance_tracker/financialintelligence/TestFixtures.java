package com.Pranav.finance_tracker.financialintelligence;

import com.Pranav.finance_tracker.analytics.dto.BudgetUsageResponse;
import com.Pranav.finance_tracker.analytics.dto.SavingTrendItem;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.financialintelligence.rules.InsightContext;
import com.Pranav.finance_tracker.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Shared builders for financial-intelligence unit tests.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("secret")
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();
    }

    public static Category category(String name) {
        return Category.builder().id(UUID.randomUUID()).name(name).isSystem(true).build();
    }

    public static Expense expense(String amount, LocalDate date, String categoryName, String title) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal(amount))
                .expenseDate(date)
                .category(category(categoryName))
                .title(title)
                .build();
    }

    public static Expense expense(String amount, LocalDate date, String categoryName) {
        return expense(amount, date, categoryName, categoryName + " purchase");
    }

    public static BudgetUsageResponse budgetUsage(String category, String budget, String spent,
                                                  String remaining, int pct, String status) {
        return BudgetUsageResponse.builder()
                .category(category)
                .budget(new BigDecimal(budget))
                .spent(new BigDecimal(spent))
                .remaining(new BigDecimal(remaining))
                .percentageUsed(pct)
                .status(status)
                .build();
    }

    public static SavingTrendItem savingTrend(int month, String amount) {
        return SavingTrendItem.builder().month(month).amount(new BigDecimal(amount)).build();
    }

    /**
     * A context builder pre-populated with sane defaults (a user, today's date, the current and
     * previous months and empty collections) so risk-rule tests only set the fields they exercise.
     */
    public static InsightContext.InsightContextBuilder riskContext() {
        LocalDate today = LocalDate.now();
        return InsightContext.builder()
                .user(user())
                .today(today)
                .currentMonth(YearMonth.from(today))
                .previousMonth(YearMonth.from(today).minusMonths(1))
                .currentMonthExpenses(List.of())
                .previousMonthExpenses(List.of())
                .windowExpenses(List.of())
                .budgetUsages(List.of())
                .savingsTrend(List.of());
    }
}
