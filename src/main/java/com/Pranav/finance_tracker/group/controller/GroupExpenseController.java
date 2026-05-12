package com.Pranav.finance_tracker.group.controller;

import com.Pranav.finance_tracker.group.dto.BalanceSummaryResponse;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.dto.GroupBalanceResponse;
import com.Pranav.finance_tracker.group.dto.UpdateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.service.GroupExpenseService;
import com.Pranav.finance_tracker.group.service.GroupBalanceService;
import com.Pranav.finance_tracker.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Group Expenses", description = "Operations related to group expenses and balances")
public class GroupExpenseController {

    private final GroupExpenseService groupExpenseService;
    private final GroupBalanceService groupBalanceService;
    private final com.Pranav.finance_tracker.auth.security.SecurityUtils securityUtils;

    @PostMapping("/api/groups/{groupId}/expenses")
    @Operation(summary = "Add a new expense to a group")
    public ResponseEntity<String> createGroupExpense(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateGroupExpenseRequest request) {
        request.setGroupId(groupId);
        groupExpenseService.createGroupExpense(request);
        return ResponseEntity.ok("Group expense created successfully");
    }

    @GetMapping("/api/groups/{groupId}/balances")
    @Operation(summary = "Calculate current balances for all group members")
    public ResponseEntity<List<GroupBalanceResponse>> getGroupBalances(
            @PathVariable UUID groupId) {
        List<GroupBalanceResponse> balances =
                groupBalanceService.calculateGroupBalance(groupId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/api/groups/{groupId}/balance-summary")
    @Operation(summary = "Get personal balance summary in a group")
    public ResponseEntity<BalanceSummaryResponse> getBalanceSummary(
            @PathVariable UUID groupId) {
        User currentUser = securityUtils.getCurrentUser();
        BalanceSummaryResponse summary =
                groupBalanceService.getBalanceSummary(groupId, currentUser);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/api/groups/expenses/{expenseId}")
    @Operation(summary = "Update an existing group expense")
    public ResponseEntity<String> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody UpdateGroupExpenseRequest request) {
        groupExpenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok("Expense updated successfully");
    }

    @DeleteMapping("/api/groups/expenses/{expenseId}")
    public ResponseEntity<String> deleteExpense(
            @PathVariable UUID expenseId) {
        groupExpenseService.deleteExpense(expenseId);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @GetMapping("/api/groups/{groupId}/expenses")
    public ResponseEntity<List<java.util.Map<String, Object>>> getGroupExpenses(
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(groupExpenseService.getGroupExpenses(groupId));
    }
}
