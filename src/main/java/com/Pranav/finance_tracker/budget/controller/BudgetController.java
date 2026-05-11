package com.Pranav.finance_tracker.budget.controller;

import com.Pranav.finance_tracker.budget.dto.CreateBudgetRequest;
import com.Pranav.finance_tracker.budget.dto.BudgetResponse;
import com.Pranav.finance_tracker.budget.service.BudgetService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<Void> createBudget(
            @RequestBody CreateBudgetRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        budgetService.createOrUpdateBudget(request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(budgetService.getBudgets(user, month, year));
    }
}

