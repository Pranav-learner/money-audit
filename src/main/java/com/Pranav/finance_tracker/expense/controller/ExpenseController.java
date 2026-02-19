package com.Pranav.finance_tracker.expense.controller;

import com.Pranav.finance_tracker.expense.PeriodType;
import com.Pranav.finance_tracker.expense.dto.*;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.service.ExpenseService;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        ExpenseResponse response =
                expenseService.createExpense(request, email);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpenses(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                expenseService.getAllExpenses(user)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable ("id") UUID id,
            @Valid  @RequestBody UpdateExpenseRequest request,
            Authentication authentication
    ){
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                expenseService.updateExpense(id, request, user)
        );
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<ExpenseResponse>> getMonthlyExpenses(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                expenseService.getMonthlyExpenses(user, year, month)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExpenseResponse>> getByCategory(
            @PathVariable UUID categoryId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                expenseService.getExpensesByCategory(user, categoryId)
        );
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                expenseService.getMonthlySummary(user, year, month)
        );
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalExpenses(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                expenseService.getTotalExpenses(user)
        );
    }

    @GetMapping("/weekly-breakdown")
    public ResponseEntity<List<DailyExpenseResponse>> weeklyBreakdown(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(expenseService.getWeeklyBreakdown(user));
    }

    @GetMapping("/trend")
    public ResponseEntity<MonthlyChange> monthlyTrend(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(expenseService.getMonthlyTrend(user));
    }


    @GetMapping("/category-distribution")
    public ResponseEntity<List<CategoryDistributionResponse>> getDistribution(
            @RequestParam(defaultValue = "MONTHLY") PeriodType period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                expenseService.getDistribution(user, period, year, month, startDate, endDate)
        );
    }


}

