package com.Pranav.finance_tracker.payment.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.group.dto.CreatePaymentRequest;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.expense.service.GroupBalanceService;
import com.Pranav.finance_tracker.payment.entity.Payment;
import com.Pranav.finance_tracker.settlement.entity.Settlement;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.settlement.repository.SettlementRepository;
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
public class GroupPaymentService {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final GroupBalanceService groupBalanceService;

    @Transactional
    public String createPayment(CreatePaymentRequest request) {

        // Idempotency check
        if (request.getRequestId() != null && !request.getRequestId().isBlank()) {
            if (paymentRepository.existsByRequestId(request.getRequestId())) {
                throw new RuntimeException("Duplicate payment request: " + request.getRequestId());
            }
        }

        User fromUser = securityUtils.getCurrentUser();

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        if (fromUser.getId().equals(toUser.getId())) {
            throw new RuntimeException("Cannot pay yourself");
        }

        if (!groupMemberRepository.existsByGroupAndUser(group, fromUser)) {
            throw new RuntimeException("Payer is not a member of this group");
        }
        if (!groupMemberRepository.existsByGroupAndUser(group, toUser)) {
            throw new RuntimeException("Receiver is not a member of this group");
        }

        BigDecimal outstandingDebt = groupBalanceService.getDebtBetweenUsers(
                group.getId(), fromUser.getId(), toUser.getId());

        if (outstandingDebt.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("You do not owe anything to this user");
        }

        if (request.getAmount().compareTo(outstandingDebt) > 0) {
            throw new RuntimeException(
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

        boolean settled = checkAndSettle(group, fromUser, toUser);

        if (settled) {
            return "Payment recorded and debt fully settled!";
        }
        return "Payment recorded successfully";
    }

    public List<Payment> getPaymentsByGroup(UUID groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return paymentRepository.findByGroupId(groupId);
    }

    private boolean checkAndSettle(Group group, User fromUser, User toUser) {

        UUID groupId = group.getId();
        UUID fromId = fromUser.getId();
        UUID toId = toUser.getId();

        List<Payment> paymentsFromTo = paymentRepository
                .findByGroupIdAndFromUserIdAndToUserId(groupId, fromId, toId);

        List<Payment> paymentsToFrom = paymentRepository
                .findByGroupIdAndFromUserIdAndToUserId(groupId, toId, fromId);

        BigDecimal totalFromTo = paymentsFromTo.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalToFrom = paymentsToFrom.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPayments = totalFromTo.subtract(totalToFrom);

        if (netPayments.compareTo(BigDecimal.ZERO) == 0 && totalFromTo.compareTo(BigDecimal.ZERO) > 0) {

            Settlement settlement = Settlement.builder()
                    .group(group)
                    .user1(fromUser)
                    .user2(toUser)
                    .totalAmount(totalFromTo)
                    .settledAt(LocalDateTime.now())
                    .build();

            settlementRepository.save(settlement);

            paymentRepository.deleteByGroupIdAndFromUserIdAndToUserId(groupId, fromId, toId);
            paymentRepository.deleteByGroupIdAndFromUserIdAndToUserId(groupId, toId, fromId);

            return true;
        }

        return false;
    }
}
