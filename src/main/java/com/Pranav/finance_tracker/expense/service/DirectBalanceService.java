package com.Pranav.finance_tracker.expense.service;

import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Computes net debt between two users for DIRECT (non-group) expenses.
 * Uses SQL aggregation — no O(n²) loops.
 *
 * Positive result → fromUser owes toUser
 * Negative result → toUser owes fromUser
 * Zero → no debt
 */
@Service
@RequiredArgsConstructor
public class DirectBalanceService {

    private final GroupExpenseSplitRepository splitRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Returns the signed net debt of fromUser toward toUser.
     * Formula: (what fromUser owes from splits) - (what toUser owes from splits)
     *          - (payments fromUser→toUser) + (payments toUser→fromUser)
     */
    public BigDecimal getDebtBetweenUsers(UUID fromUserId, UUID toUserId) {

        // What fromUser owes toUser (toUser paid, fromUser's split)
        BigDecimal fromOwesToTo = splitRepository.sumDirectDebt(fromUserId, toUserId);

        // What toUser owes fromUser (fromUser paid, toUser's split)
        BigDecimal toOwesFromFrom = splitRepository.sumDirectDebt(toUserId, fromUserId);

        // Payments made: fromUser → toUser
        BigDecimal paidFromTo = paymentRepository.sumDirectPaymentsFromTo(fromUserId, toUserId);

        // Payments made: toUser → fromUser
        BigDecimal paidToFrom = paymentRepository.sumDirectPaymentsFromTo(toUserId, fromUserId);

        // Net debt = what I owe them - what they owe me - what I've paid + what they've paid back
        return fromOwesToTo.subtract(toOwesFromFrom)
                .subtract(paidFromTo)
                .add(paidToFrom);
    }
}
