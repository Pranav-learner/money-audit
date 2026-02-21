package com.Pranav.finance_tracker.expense.repository;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.expense.dto.DailyExpenseResponse;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);

    List<Expense> findByUserAndExpenseDateBetween(
            User user,
            LocalDate start,
            LocalDate end
    );

    Optional<Expense> findByIdAndUser(UUID id, User user);

    List<Expense> findByUserAndCategoryId(User user, UUID categoryId);

    BigDecimal sumByUserAndExpenseDateBetween(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
            );

    @Query("""
            SELECT SUM(e.amount)
            from Expense e
            WHERE e.user = :user
            AND e.category = :category
            AND e.expenseDate BETWEEN :start and :end
            """)
    BigDecimal sumByUserAndCategoryAndExpenseDateBetween(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    Long countByUserAndExpenseDateBetween(
            User user,
            LocalDate start,
            LocalDate end
    );

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user")
    BigDecimal sumAmountByUser(@Param("user") User user);

    @Query("""
       SELECT new com.Pranav.finance_tracker.expense.dto.DailyExpenseResponse(
           e.expenseDate,
           SUM(e.amount)
       )
       FROM Expense e
       WHERE e.user = :user
       AND e.expenseDate BETWEEN :start AND :end
       GROUP BY e.expenseDate
       ORDER BY e.expenseDate
       """)
    List<DailyExpenseResponse> getDailyBreakdown(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
       SELECT new com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse(
           e.category.name,
           SUM(e.amount)
       )
       FROM Expense e
       WHERE e.user = :user
       AND e.expenseDate BETWEEN :start AND :end
       GROUP BY e.category.name
       """)
    List<CategoryDistributionResponse> getCategoryDistribution(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT new com.Pranav.finance_tracker.analytics.dto.SavingTrendItem(
            FUNCTION('MONTH', e.expenseDate),
            SUM(e.amount)
        )
        FROM Expense e
        WHERE e.user = :user
        AND FUNCTION('YEAR', e.expenseDate) = :year
        GROUP BY FUNCTION('MONTH', e.expenseDate)
        ORDER BY FUNCTION('MONTH', e.expenseDate)
    """)
    List<com.Pranav.finance_tracker.analytics.dto.SavingTrendItem> getMonthlySpendingTrend(
            @Param("user") User user,
            @Param("year") int year
    );
}
