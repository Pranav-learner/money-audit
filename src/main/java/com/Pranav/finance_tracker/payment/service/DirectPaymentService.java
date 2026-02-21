package com.Pranav.finance_tracker.payment.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.expense.service.DirectBalanceService;
import com.Pranav.finance_tracker.payment.dto.CreateDirectPaymentRequest;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectPaymentService {

    private final PaymentRepository paymentRepository;
    private final DirectBalanceService directBalanceService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;

    @Transactional
    public String createPayment(CreateDirectPaymentRequest request) {

        if (request.getRequestId() != null && !request.getRequestId().isBlank()) {
            if (paymentRepository.existsByRequestId(request.getRequestId())) {
                throw new RuntimeException("Duplicate payment request: " + request.getRequestId());
            }
        }

        User fromUser = securityUtils.getCurrentUser();

        User toUser = userRepository.findByPhone(request.getToUserPhone())
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + request.getToUserPhone()));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        if (fromUser.getId().equals(toUser.getId())) {
            throw new RuntimeException("Cannot pay yourself");
        }

        BigDecimal debt = directBalanceService.getDebtBetweenUsers(fromUser.getId(), toUser.getId());

        if (debt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("You do not owe anything to this user");
        }

        if (request.getAmount().compareTo(debt) > 0) {
            throw new RuntimeException(
                    "Cannot overpay: you owe " + debt + " but tried to pay " + request.getAmount());
        }

        Payment payment = Payment.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(request.getAmount())
                .group(null)
                .note(request.getNote())
                .requestId(request.getRequestId())
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Send Notification
        String subject = "Payment Received: " + payment.getAmount();
        String body = String.format("Hello %s,\n\n%s has paid you %.2f directly.\nNote: %s",
                toUser.getName(), fromUser.getName(), payment.getAmount(),
                (payment.getNote() != null ? payment.getNote() : "No note"));
        emailService.sendEmail(toUser, subject, body);

        return "Payment recorded successfully";
    }

    public List<Payment> getPaymentHistory(String otherUserPhone) {
        User currentUser = securityUtils.getCurrentUser();
        User otherUser = userRepository.findByPhone(otherUserPhone)
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + otherUserPhone));
        return paymentRepository.findDirectPaymentsBetween(currentUser.getId(), otherUser.getId());
    }

    public BigDecimal getBalance(String otherUserPhone) {
        User currentUser = securityUtils.getCurrentUser();
        User otherUser = userRepository.findByPhone(otherUserPhone)
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + otherUserPhone));
        return directBalanceService.getDebtBetweenUsers(currentUser.getId(), otherUser.getId());
    }

}
