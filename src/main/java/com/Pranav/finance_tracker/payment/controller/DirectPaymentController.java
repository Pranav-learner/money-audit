package com.Pranav.finance_tracker.payment.controller;

import com.Pranav.finance_tracker.payment.dto.CreateDirectPaymentRequest;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.service.DirectPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DirectPaymentController {

    private final DirectPaymentService directPaymentService;

    @PostMapping("/api/direct-payments")
    public ResponseEntity<String> createPayment(
            @RequestBody CreateDirectPaymentRequest request) {
        return ResponseEntity.ok(directPaymentService.createPayment(request));
    }

    @GetMapping("/api/direct-payments/history")
    public ResponseEntity<List<Payment>> getPaymentHistory(
            @RequestParam String phone) {
        return ResponseEntity.ok(directPaymentService.getPaymentHistory(phone));
    }

    @GetMapping("/api/direct-balance")
    public ResponseEntity<BigDecimal> getBalance(@RequestParam String phone) {
        return ResponseEntity.ok(directPaymentService.getBalance(phone));
    }
}
