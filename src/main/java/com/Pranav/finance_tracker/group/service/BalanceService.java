package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.group.dto.BalanceSummaryResponse;
import com.Pranav.finance_tracker.group.dto.GroupBalanceResponse;
import com.Pranav.finance_tracker.group.dto.MemberBalanceDetail;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.group.entity.Payment;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseSplitRepository splitRepository;
    private final PaymentRepository paymentRepository;

    // ── Existing: per-user net balance for the whole group ──

    public List<GroupBalanceResponse> calculateGroupBalance(UUID groupId) {

        Map<UUID, String> nameMap = new HashMap<>();
        Map<UUID, BigDecimal> netMap = buildNetBalanceMap(groupId, nameMap);

        List<GroupBalanceResponse> response = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : netMap.entrySet()) {
            response.add(
                    new GroupBalanceResponse(
                            entry.getKey(),
                            nameMap.get(entry.getKey()),
                            entry.getValue()
                    )
            );
        }

        return response;
    }

    // ── NEW: Balance Summary for current user ──

    public BalanceSummaryResponse getBalanceSummary(UUID groupId, User currentUser) {

        UUID myId = currentUser.getId();
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);

        // Build per-pair net: how much each member owes/is owed by currentUser
        // pairNet positive → that user owes currentUser
        // pairNet negative → currentUser owes that user
        Map<UUID, BigDecimal> pairNet = new HashMap<>();
        Map<UUID, String> nameMap = new HashMap<>();

        for (GroupMember member : members) {
            UUID uid = member.getUser().getId();
            if (!uid.equals(myId)) {
                pairNet.put(uid, BigDecimal.ZERO);
                nameMap.put(uid, member.getUser().getName());
            }
        }

        // ─ Expenses: payer paid, each split user owes ─
        List<GroupExpense> expenses = groupExpenseRepository.findByGroupId(groupId);
        List<GroupExpenseSplit> splits = splitRepository.findByExpenseGroupId(groupId);

        // For each expense, the payer is owed by each split user
        for (GroupExpense expense : expenses) {
            UUID payerId = expense.getPaidBy().getId();

            // Find splits for this expense
            for (GroupExpenseSplit split : splits) {
                if (!split.getExpense().getId().equals(expense.getId())) continue;

                UUID owerId = split.getUser().getId();
                if (payerId.equals(owerId)) continue; // skip self-split

                BigDecimal amount = split.getAmountOwed();

                if (payerId.equals(myId)) {
                    // I paid, ower owes me → positive for ower
                    pairNet.merge(owerId, amount, BigDecimal::add);
                } else if (owerId.equals(myId)) {
                    // Someone else paid, I owe them → negative for payer
                    pairNet.merge(payerId, amount.negate(), BigDecimal::add);
                }
            }
        }

        // ─ Payments: fromUser paid toUser, reduces debt ─
        List<Payment> payments = paymentRepository.findByGroupId(groupId);

        for (Payment payment : payments) {
            UUID fromId = payment.getFromUser().getId();
            UUID toId = payment.getToUser().getId();
            BigDecimal amount = payment.getAmount();

            if (fromId.equals(myId)) {
                // I paid someone → reduces what I owe them (more negative → less negative)
                pairNet.merge(toId, amount.negate(), BigDecimal::add);
            } else if (toId.equals(myId)) {
                // Someone paid me → reduces what they owe me (more positive → less positive)
                pairNet.merge(fromId, amount.negate(), BigDecimal::add);
            }
        }

        // ─ Build response ─
        BigDecimal netBalance = BigDecimal.ZERO;
        List<MemberBalanceDetail> balances = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : pairNet.entrySet()) {
            UUID userId = entry.getKey();
            BigDecimal net = entry.getValue();

            netBalance = netBalance.add(net);

            MemberBalanceDetail.BalanceType type;
            BigDecimal displayAmount;

            if (net.compareTo(BigDecimal.ZERO) > 0) {
                type = MemberBalanceDetail.BalanceType.YOU_ARE_OWED;
                displayAmount = net;
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                type = MemberBalanceDetail.BalanceType.YOU_OWE;
                displayAmount = net.abs();
            } else {
                type = MemberBalanceDetail.BalanceType.SETTLED;
                displayAmount = BigDecimal.ZERO;
            }

            balances.add(MemberBalanceDetail.builder()
                    .userId(userId)
                    .userName(nameMap.get(userId))
                    .amount(displayAmount)
                    .type(type)
                    .build());
        }

        return BalanceSummaryResponse.builder()
                .groupId(groupId)
                .netBalance(netBalance)
                .balances(balances)
                .build();
    }

    // ── NEW: Check if group has unsettled balances ──

    public boolean hasUnsettledBalances(UUID groupId) {

        Map<UUID, BigDecimal> netMap = buildNetBalanceMap(groupId, null);

        for (BigDecimal net : netMap.values()) {
            if (net.compareTo(BigDecimal.ZERO) != 0) {
                return true;
            }
        }
        return false;
    }

    // ── Internal: compute net balance per user ──

    private Map<UUID, BigDecimal> buildNetBalanceMap(UUID groupId, Map<UUID, String> nameMap) {

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<GroupExpense> expenses = groupExpenseRepository.findByGroupId(groupId);
        List<GroupExpenseSplit> splits = splitRepository.findByExpenseGroupId(groupId);
        List<Payment> payments = paymentRepository.findByGroupId(groupId);

        Map<UUID, BigDecimal> paidMap = new HashMap<>();
        Map<UUID, BigDecimal> owedMap = new HashMap<>();

        for (GroupMember member : members) {
            UUID uid = member.getUser().getId();
            paidMap.put(uid, BigDecimal.ZERO);
            owedMap.put(uid, BigDecimal.ZERO);
            if (nameMap != null) {
                nameMap.put(uid, member.getUser().getName());
            }
        }

        for (GroupExpense expense : expenses) {
            UUID payerId = expense.getPaidBy().getId();
            paidMap.put(payerId, paidMap.get(payerId).add(expense.getTotalAmount()));
        }

        for (GroupExpenseSplit split : splits) {
            UUID userId = split.getUser().getId();
            owedMap.put(userId, owedMap.get(userId).add(split.getAmountOwed()));
        }

        for (Payment payment : payments) {
            UUID fromId = payment.getFromUser().getId();
            UUID toId = payment.getToUser().getId();
            BigDecimal amount = payment.getAmount();

            paidMap.put(fromId, paidMap.getOrDefault(fromId, BigDecimal.ZERO).add(amount));
            owedMap.put(toId, owedMap.getOrDefault(toId, BigDecimal.ZERO).add(amount));
        }

        Map<UUID, BigDecimal> netMap = new HashMap<>();
        for (UUID uid : paidMap.keySet()) {
            netMap.put(uid, paidMap.get(uid).subtract(owedMap.get(uid)));
        }

        return netMap;
    }

    // ── NEW: Compute debt between two specific users ──

    /**
     * Returns how much fromUser owes toUser in this group.
     * Positive = fromUser owes toUser that amount.
     * Zero or negative = fromUser does not owe toUser.
     */
    public BigDecimal getDebtBetweenUsers(UUID groupId, UUID fromUserId, UUID toUserId) {

        List<GroupExpense> expenses = groupExpenseRepository.findByGroupId(groupId);
        List<GroupExpenseSplit> splits = splitRepository.findByExpenseGroupId(groupId);
        List<Payment> payments = paymentRepository.findByGroupId(groupId);

        BigDecimal debt = BigDecimal.ZERO;

        // Expenses: if toUser paid and fromUser has a split → fromUser owes toUser
        //           if fromUser paid and toUser has a split → toUser owes fromUser (reduces debt)
        for (GroupExpense expense : expenses) {
            UUID payerId = expense.getPaidBy().getId();

            for (GroupExpenseSplit split : splits) {
                if (!split.getExpense().getId().equals(expense.getId())) continue;

                UUID owerId = split.getUser().getId();
                if (payerId.equals(owerId)) continue; // skip self-split

                if (payerId.equals(toUserId) && owerId.equals(fromUserId)) {
                    // toUser paid, fromUser owes → increases debt
                    debt = debt.add(split.getAmountOwed());
                } else if (payerId.equals(fromUserId) && owerId.equals(toUserId)) {
                    // fromUser paid, toUser owes → decreases debt
                    debt = debt.subtract(split.getAmountOwed());
                }
            }
        }

        // Payments: fromUser→toUser reduces debt, toUser→fromUser increases debt
        for (Payment payment : payments) {
            UUID pFromId = payment.getFromUser().getId();
            UUID pToId = payment.getToUser().getId();

            if (pFromId.equals(fromUserId) && pToId.equals(toUserId)) {
                debt = debt.subtract(payment.getAmount());
            } else if (pFromId.equals(toUserId) && pToId.equals(fromUserId)) {
                debt = debt.add(payment.getAmount());
            }
        }

        // If debt is negative, fromUser doesn't owe toUser
        return debt.max(BigDecimal.ZERO);
    }
}
