package com.Pranav.finance_tracker.payment.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.payment.dto.RazorpayOrderRequest;
import com.Pranav.finance_tracker.payment.dto.RazorpayVerifyRequest;
import com.Pranav.finance_tracker.payment.service.DirectPaymentService;
import com.Pranav.finance_tracker.payment.service.RazorpayService;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/razorpay")
@RequiredArgsConstructor
@Tag(name = "Payments (Razorpay)", description = "Razorpay order creation and verification")
public class RazorpayController {

    private final RazorpayService razorpayService;
    private final DirectPaymentService directPaymentService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay order")
    public ResponseEntity<Map<String, String>> createOrder(@RequestBody RazorpayOrderRequest request) {
        String orderId = razorpayService.createOrder(request.getAmount(), null);
        return ResponseEntity.ok(Map.of("orderId", orderId));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment and record it")
    public ResponseEntity<String> verifyPayment(@RequestBody RazorpayVerifyRequest request) {
        boolean isValid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid payment signature");
        }

        User fromUser = securityUtils.getCurrentUser();
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        directPaymentService.recordRazorpayPayment(
                fromUser,
                toUser,
                request.getAmount(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getNote()
        );

        return ResponseEntity.ok("Payment verified and recorded successfully");
    }
}
