package com.Pranav.finance_tracker.friend.controller;

import com.Pranav.finance_tracker.friend.dto.CreateDirectExpenseRequest;
import com.Pranav.finance_tracker.friend.dto.DirectTransactionResponse;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.friend.service.DirectExpenseService;
import com.Pranav.finance_tracker.payment.dto.CreateDirectPaymentRequest;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.service.DirectPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Direct Split", description = "1-to-1 expenses and settlements")
public class DirectSplitController {

    private final DirectExpenseService directExpenseService;
    private final DirectPaymentService directPaymentService;

    @GetMapping("/{friendId}")
    @Operation(summary = "Get transaction history between current user and a friend")
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
    @Operation(summary = "Add a new 1-to-1 expense")
    public ResponseEntity<String> addDirectExpense(@Valid @RequestBody CreateDirectExpenseRequest request) {
        return ResponseEntity.ok(directExpenseService.createExpense(request));
    }

    @PostMapping("/settle")
    @Operation(summary = "Settle 1-to-1 debt (manual/mock payment)")
    public ResponseEntity<String> settleDirect(@Valid @RequestBody CreateDirectPaymentRequest request) {
        return ResponseEntity.ok(directPaymentService.createPayment(request));
    }

    @GetMapping("/{friendId}/balance")
    public ResponseEntity<java.math.BigDecimal> getBalance(@PathVariable UUID friendId) {
        return ResponseEntity.ok(directPaymentService.getBalanceByUserId(friendId));
    }
}
