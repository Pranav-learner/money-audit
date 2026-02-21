package com.Pranav.finance_tracker.expense.controller;

import com.Pranav.finance_tracker.group.dto.BalanceSummaryResponse;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.dto.GroupBalanceResponse;
import com.Pranav.finance_tracker.group.dto.UpdateGroupExpenseRequest;
import com.Pranav.finance_tracker.expense.service.GroupExpenseService;
import com.Pranav.finance_tracker.expense.service.GroupBalanceService;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GroupExpenseController {

    private final GroupExpenseService groupExpenseService;
    private final GroupBalanceService groupBalanceService;

    @PostMapping("/api/groups/{groupId}/expenses")
    public ResponseEntity<String> createGroupExpense(
            @PathVariable UUID groupId,
            @RequestBody CreateGroupExpenseRequest request) {
        request.setGroupId(groupId);
        groupExpenseService.createGroupExpense(request);
        return ResponseEntity.ok("Group expense created successfully");
    }

    @GetMapping("/api/groups/{groupId}/balances")
    public ResponseEntity<List<GroupBalanceResponse>> getGroupBalances(
            @PathVariable UUID groupId) {
        List<GroupBalanceResponse> balances =
                groupBalanceService.calculateGroupBalance(groupId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/api/groups/{groupId}/balance-summary")
    public ResponseEntity<BalanceSummaryResponse> getBalanceSummary(
            @PathVariable UUID groupId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        BalanceSummaryResponse summary =
                groupBalanceService.getBalanceSummary(groupId, currentUser);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/api/groups/expenses/{expenseId}")
    public ResponseEntity<String> updateExpense(
            @PathVariable UUID expenseId,
            @RequestBody UpdateGroupExpenseRequest request) {
        groupExpenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok("Expense updated successfully");
    }

    @DeleteMapping("/api/groups/expenses/{expenseId}")
    public ResponseEntity<String> deleteExpense(
            @PathVariable UUID expenseId) {
        groupExpenseService.deleteExpense(expenseId);
        return ResponseEntity.ok("Expense deleted successfully");
    }
}
