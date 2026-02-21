package com.Pranav.finance_tracker.payment.controller;

import com.Pranav.finance_tracker.group.dto.CreatePaymentRequest;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.service.GroupPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class GroupPaymentController {

    private final GroupPaymentService groupPaymentService;

    @PostMapping
    public ResponseEntity<String> createPayment(
            @RequestBody CreatePaymentRequest request) {

        String message = groupPaymentService.createPayment(request);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Payment>> getPaymentsByGroup(
            @PathVariable UUID groupId) {

        List<Payment> payments = groupPaymentService.getPaymentsByGroup(groupId);
        return ResponseEntity.ok(payments);
    }
}
