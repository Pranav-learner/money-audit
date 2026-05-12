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
    private final com.Pranav.finance_tracker.auth.security.SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<Void> createBudget(
            @RequestBody CreateBudgetRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        budgetService.createOrUpdateBudget(request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(budgetService.getBudgets(user, month, year));
    }
}

