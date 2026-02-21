package com.Pranav.finance_tracker.expense.controller;

import com.Pranav.finance_tracker.expense.dto.CreateDirectExpenseRequest;
import com.Pranav.finance_tracker.expense.service.DirectExpenseService;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DirectExpenseController {

    private final DirectExpenseService directExpenseService;

    @PostMapping("/api/direct-expenses")
    public ResponseEntity<String> createExpense(
            @RequestBody CreateDirectExpenseRequest request) {
        return ResponseEntity.ok(directExpenseService.createExpense(request));
    }

    @GetMapping("/api/direct-expenses/history")
    public ResponseEntity<List<GroupExpense>> getExpenseHistory(
            @RequestParam String phone) {
        return ResponseEntity.ok(directExpenseService.getExpenseHistory(phone));
    }
}
