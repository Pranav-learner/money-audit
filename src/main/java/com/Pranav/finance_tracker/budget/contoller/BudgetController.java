package com.Pranav.finance_tracker.budget.contoller;

import com.Pranav.finance_tracker.budget.dto.CreateBudgetRequest;
import com.Pranav.finance_tracker.budget.service.BudgetService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

