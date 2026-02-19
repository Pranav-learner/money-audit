package com.Pranav.finance_tracker.expense.service;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.expense.PeriodType;
import com.Pranav.finance_tracker.expense.dto.*;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ExpenseResponse createExpense(CreateExpenseRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow();

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .description(request.getDescription())
                .user(user)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        expenseRepository.save(expense);

        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .categoryName(category.getName())
                .build();
    }
    public List<ExpenseResponse> getAllExpenses(User user) {

        return expenseRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    public ExpenseResponse updateExpense(
            UUID expenseId,
            UpdateExpenseRequest request,
            User user
    ) {

        Expense expense = expenseRepository
                .findByIdAndUser(expenseId, user)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (request.getTitle() != null)
            expense.setTitle(request.getTitle());

        if (request.getAmount() != null)
            expense.setAmount(request.getAmount());

        if (request.getExpenseDate() != null)
            expense.setExpenseDate(request.getExpenseDate());

        if (request.getDescription() != null)
            expense.setDescription(request.getDescription());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            expense.setCategory(category);
        }

        expenseRepository.save(expense);

        return mapToResponse(expense);
    }

    public List<ExpenseResponse> getMonthlyExpenses(
            User user,
            int year,
            int month
    ) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return expenseRepository
                .findByUserAndExpenseDateBetween(user, start, end)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ExpenseResponse> getExpensesByCategory(
            User user,
            UUID categoryId
    ) {

        return expenseRepository
                .findByUserAndCategoryId(user, categoryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public MonthlySummaryResponse getMonthlySummary(
            User user,
            int year,
            int month
    ) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        BigDecimal total = expenseRepository
                .sumByUserAndExpenseDateBetween(user, start, end);

        Long count = expenseRepository
                .countByUserAndExpenseDateBetween(user, start, end);

        return new MonthlySummaryResponse(
                total != null ? total : BigDecimal.ZERO,
                count
        );

    }

    public BigDecimal getTotalExpenses(User user) {

        BigDecimal total = expenseRepository.sumAmountByUser(user);

        return total != null ? total : BigDecimal.ZERO;
    }

    public List<DailyExpenseResponse> getWeeklyBreakdown(User user){
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);

        List<DailyExpenseResponse> result =
                expenseRepository.getDailyBreakdown(user, start, end);

        // Ensure missing days are filled with zero
        Map<LocalDate, BigDecimal> map = result.stream()
                .collect(Collectors.toMap(
                        DailyExpenseResponse::getDate,
                        DailyExpenseResponse::getTotalAmount
                ));

        List<DailyExpenseResponse> finalResult = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = start.plusDays(i);
            finalResult.add(new DailyExpenseResponse(
                    date,
                    map.getOrDefault(date, BigDecimal.ZERO)
            ));
        }

        return finalResult;
    }

    public MonthlyChange getMonthlyTrend(User user) {

        LocalDate now = LocalDate.now();

        LocalDate currentStart = now.withDayOfMonth(1);
        LocalDate currentEnd = currentStart.withDayOfMonth(currentStart.lengthOfMonth());

        LocalDate lastStart = currentStart.minusMonths(1);
        LocalDate lastEnd = lastStart.withDayOfMonth(lastStart.lengthOfMonth());

        BigDecimal current = expenseRepository
                .sumByUserAndExpenseDateBetween(user, currentStart, currentEnd);

        BigDecimal last = expenseRepository
                .sumByUserAndExpenseDateBetween(user, lastStart, lastEnd);

        current = current != null ? current : BigDecimal.ZERO;
        last = last != null ? last : BigDecimal.ZERO;

        double percentage = last.compareTo(BigDecimal.ZERO) == 0
                ? 100
                : current.subtract(last)
                .divide(last, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return new MonthlyChange(current, last, percentage);
    }

    public List<CategoryDistributionResponse> getDistribution(
            User user,
            PeriodType period,
            Integer year,
            Integer month,
            LocalDate startDate,
            LocalDate endDate
    ) {

        LocalDate start;
        LocalDate end;

        switch (period) {

            case WEEKLY -> {
                start = LocalDate.now().with(DayOfWeek.MONDAY);
                end = start.plusDays(6);
            }

            case MONTHLY -> {
                int y = year != null ? year : LocalDate.now().getYear();
                int m = month != null ? month : LocalDate.now().getMonthValue();

                start = LocalDate.of(y, m, 1);
                end = start.withDayOfMonth(start.lengthOfMonth());
            }

            case CUSTOM -> {
                if (startDate == null || endDate == null) {
                    throw new RuntimeException("Start and end date required for custom period");
                }
                start = startDate;
                end = endDate;
            }

            default -> throw new RuntimeException("Invalid period type");
        }

        return expenseRepository.getCategoryDistribution(user, start, end);
    }





    private ExpenseResponse mapToResponse(Expense expense){
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .categoryName(expense.getCategory().getName())
                .description(expense.getDescription())
                .build();
    }


}
