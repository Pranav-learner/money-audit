package com.Pranav.finance_tracker.group.controller;

import com.Pranav.finance_tracker.group.dto.CreatePaymentRequest;
import com.Pranav.finance_tracker.group.entity.Payment;
import com.Pranav.finance_tracker.group.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<String> createPayment(
            @RequestBody CreatePaymentRequest request) {

        String message = paymentService.createPayment(request);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Payment>> getPaymentsByGroup(
            @PathVariable UUID groupId) {

        List<Payment> payments = paymentService.getPaymentsByGroup(groupId);
        return ResponseEntity.ok(payments);
    }
}
