package com.Pranav.finance_tracker.friend.service;

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
        BigDecimal fromOwes = splitRepository.sumDirectDebt(fromUserId, toUserId);
        BigDecimal toOwes = splitRepository.sumDirectDebt(toUserId, fromUserId);
        BigDecimal paidFromTo = paymentRepository.sumDirectPaymentsFromTo(fromUserId, toUserId);
        BigDecimal paidToFrom = paymentRepository.sumDirectPaymentsFromTo(toUserId, fromUserId);

        BigDecimal fromOwesVal = fromOwes != null ? fromOwes : BigDecimal.ZERO;
        BigDecimal toOwesVal = toOwes != null ? toOwes : BigDecimal.ZERO;
        BigDecimal paidFromToVal = paidFromTo != null ? paidFromTo : BigDecimal.ZERO;
        BigDecimal paidToFromVal = paidToFrom != null ? paidToFrom : BigDecimal.ZERO;

        return fromOwesVal.subtract(toOwesVal)
                .subtract(paidFromToVal)
                .add(paidToFromVal);
    }
}
