package com.Pranav.finance_tracker.payment.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.group.dto.CreatePaymentRequest;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.group.service.GroupBalanceService;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupPaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final GroupBalanceService groupBalanceService;
    private final EmailService emailService;

    @Transactional
    public String createPayment(CreatePaymentRequest request) {

        // Idempotency check
        if (request.getRequestId() != null && !request.getRequestId().isBlank()) {
            if (paymentRepository.existsByRequestId(request.getRequestId())) {
                throw new com.Pranav.finance_tracker.exception.BadRequestException("Duplicate payment request: " + request.getRequestId());
            }
        }

        User fromUser = securityUtils.getCurrentUser();

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new com.Pranav.finance_tracker.exception.ResourceNotFoundException("Group not found"));

        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new com.Pranav.finance_tracker.exception.ResourceNotFoundException("User not found"));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException("Amount must be positive");
        }

        if (fromUser.getId().equals(toUser.getId())) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException("Cannot pay yourself");
        }

        if (!groupMemberRepository.existsByGroupAndUser(group, fromUser)) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException("Payer is not a member of this group");
        }
        if (!groupMemberRepository.existsByGroupAndUser(group, toUser)) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException("Receiver is not a member of this group");
        }

        BigDecimal outstandingDebt = groupBalanceService.getDebtBetweenUsers(
                group.getId(), fromUser.getId(), toUser.getId());

        if (outstandingDebt.compareTo(BigDecimal.ZERO) == 0) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException("You do not owe anything to this user");
        }

        if (request.getAmount().compareTo(outstandingDebt) > 0) {
            throw new com.Pranav.finance_tracker.exception.BadRequestException(
                    "Cannot overpay: you owe " + outstandingDebt +
                    " but tried to pay " + request.getAmount());
        }

        Payment payment = Payment.builder()
                .group(group)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(request.getAmount())
                .note(request.getNote())
                .requestId(request.getRequestId())
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Send Notification after transaction commits successfully
        final User finalToUser = toUser;
        final User finalFromUser = fromUser;
        final Group finalGroup = group;
        final Payment finalPayment = payment;

        Runnable sendEmailTask = () -> {
            String subject = "Payment Received: " + finalPayment.getAmount();
            String body = String.format("Hello %s,\n\n%s has paid you %.2f in group '%s'.\nNote: %s",
                    finalToUser.getName(), finalFromUser.getName(), finalPayment.getAmount(),
                    finalGroup.getName(), (finalPayment.getNote() != null ? finalPayment.getNote() : "No note"));
            emailService.sendEmail(finalToUser, subject, body);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmailTask.run();
                }
            });
        } else {
            sendEmailTask.run();
        }

        return "Payment recorded successfully";
    }

    public List<Payment> getPaymentsByGroup(UUID groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return paymentRepository.findByGroupId(groupId);
    }

}
