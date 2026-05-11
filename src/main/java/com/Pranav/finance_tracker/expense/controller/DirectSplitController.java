package com.Pranav.finance_tracker.expense.controller;

import com.Pranav.finance_tracker.expense.dto.CreateDirectExpenseRequest;
import com.Pranav.finance_tracker.expense.dto.DirectTransactionResponse;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.expense.service.DirectExpenseService;
import com.Pranav.finance_tracker.payment.dto.CreateDirectPaymentRequest;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.service.DirectPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct")
@RequiredArgsConstructor
public class DirectSplitController {

    private final DirectExpenseService directExpenseService;
    private final DirectPaymentService directPaymentService;

    @GetMapping("/{friendId}")
    public ResponseEntity<List<DirectTransactionResponse>> getDirectHistory(@PathVariable UUID friendId) {
        List<GroupExpense> expenses = directExpenseService.getExpenseHistoryByUserId(friendId);
        List<Payment> payments = directPaymentService.getPaymentHistoryByUserId(friendId);

        List<DirectTransactionResponse> transactions = new ArrayList<>();

        for (GroupExpense e : expenses) {
            transactions.add(DirectTransactionResponse.builder()
                    .id(e.getId())
                    .description(e.getTitle())
                    .amount(e.getTotalAmount())
                    .paidByUserId(e.getPaidBy().getId())
                    .date(e.getCreatedAt())
                    .type("EXPENSE")
                    .build());
        }

        for (Payment p : payments) {
            transactions.add(DirectTransactionResponse.builder()
                    .id(p.getId())
                    .description(p.getNote() != null ? p.getNote() : "Payment Settlement")
                    .amount(p.getAmount())
                    .paidByUserId(p.getFromUser().getId())
                    .date(p.getCreatedAt())
                    .type("PAYMENT")
                    .build());
        }

        transactions.sort(Comparator.comparing(DirectTransactionResponse::getDate).reversed());

        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<String> addDirectExpense(@RequestBody CreateDirectExpenseRequest request) {
        return ResponseEntity.ok(directExpenseService.createExpense(request));
    }

    @PostMapping("/settle")
    public ResponseEntity<String> settleDirect(@RequestBody CreateDirectPaymentRequest request) {
        return ResponseEntity.ok(directPaymentService.createPayment(request));
    }

    @GetMapping("/{friendId}/balance")
    public ResponseEntity<java.math.BigDecimal> getBalance(@PathVariable UUID friendId) {
        return ResponseEntity.ok(directPaymentService.getBalanceByUserId(friendId));
    }
}
