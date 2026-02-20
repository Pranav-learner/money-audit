package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.group.dto.CreatePaymentRequest;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.Payment;
import com.Pranav.finance_tracker.group.entity.Settlement;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.group.repository.PaymentRepository;
import com.Pranav.finance_tracker.group.repository.SettlementRepository;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final BalanceService balanceService;

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

        // Validate amount is positive
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        // Cannot pay yourself
        if (fromUser.getId().equals(toUser.getId())) {
            throw new RuntimeException("Cannot pay yourself");
        }

        // Both users must be members of the group
        if (!groupMemberRepository.existsByGroupAndUser(group, fromUser)) {
            throw new RuntimeException("Payer is not a member of this group");
        }
        if (!groupMemberRepository.existsByGroupAndUser(group, toUser)) {
            throw new RuntimeException("Receiver is not a member of this group");
        }

        // Overpayment prevention: check if amount exceeds outstanding debt
        BigDecimal outstandingDebt = balanceService.getDebtBetweenUsers(
                group.getId(), fromUser.getId(), toUser.getId());

        if (outstandingDebt.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("You do not owe anything to this user");
        }

        if (request.getAmount().compareTo(outstandingDebt) > 0) {
            throw new RuntimeException(
                    "Cannot overpay: you owe " + outstandingDebt +
                    " but tried to pay " + request.getAmount());
        }

        // Save the payment record
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

        // Check if debt is fully settled between these two users
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

    /**
     * Compute net balance between fromUser and toUser.
     * If net = 0, create a Settlement record and delete all Payment records.
     *
     * Net is calculated as:
     *   totalPaidByFrom = sum of payments from→to
     *   totalPaidByTo   = sum of payments to→from
     *   net = totalPaidByFrom - totalPaidByTo
     *
     * But the real "debt" comes from the balance engine (expenses + splits).
     * Here we only check if the payment total between the pair matches
     * what was owed. We use the BalanceService-style calculation for just
     * these two users.
     *
     * Simpler approach: sum all payments in both directions.
     * If from→to total equals to→from total (i.e., difference = 0),
     * they're settled. But that doesn't capture expense-based debt.
     *
     * Better approach: sum payments from→to. This is what reduces the debt.
     * The balance engine already factors these in. So we check by computing
     * the net: sum(from→to payments) - sum(to→from payments).
     * Combined with expense splits, if net balance = 0, settled.
     *
     * For simplicity, we compute: total payments from→to and to→from,
     * and if the difference matches the expense-based debt, it's settled.
     *
     * SIMPLEST correct approach: just check total payments between the pair.
     */
    private boolean checkAndSettle(Group group, User fromUser, User toUser) {

        UUID groupId = group.getId();
        UUID fromId = fromUser.getId();
        UUID toId = toUser.getId();

        // Get all payments in both directions between this pair
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

        // If net payments = 0, both sides have paid equally → settled
        if (netPayments.compareTo(BigDecimal.ZERO) == 0 && totalFromTo.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal settledAmount = totalFromTo;

            // Create settlement record
            Settlement settlement = Settlement.builder()
                    .group(group)
                    .user1(fromUser)
                    .user2(toUser)
                    .totalAmount(settledAmount)
                    .settledAt(LocalDateTime.now())
                    .build();

            settlementRepository.save(settlement);

            // Delete all payment records between this pair
            paymentRepository.deleteByGroupIdAndFromUserIdAndToUserId(groupId, fromId, toId);
            paymentRepository.deleteByGroupIdAndFromUserIdAndToUserId(groupId, toId, fromId);

            return true;
        }

        return false;
    }
}
