package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.dto.CreateGroupRequest;
import com.Pranav.finance_tracker.group.dto.SplitDetail;
import com.Pranav.finance_tracker.group.dto.UpdateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.entity.GroupMember;
import com.Pranav.finance_tracker.group.enums.SplitType;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.group.repository.GroupMemberRepository;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.group.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CreateGroupExpenseService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseSplitRepository groupExpenseSplitRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void createGroupExpense(CreateGroupExpenseRequest request ){

        User currenntUser = securityUtils.getCurrentUser();

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(()-> new RuntimeException("Group not found"));

        validateUserIsGroupMember(group, currenntUser);

        // Validate total amount is positive
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        GroupExpense expense = createExpense(request,group, currenntUser);

        switch (request.getSplitType()) {

            case EQUAL -> handleEqualSplit(expense, group);

            case UNEQUAL -> handleUnequalSplit(expense, group, request.getSplits());

            case PERCENTAGE -> handlePercentageSplit(expense,group,request.getSplits());
        }

    }

    private void validateUserIsGroupMember(Group group, User user) {

        boolean isMember = groupMemberRepository
                .existsByGroupAndUser(group, user);

        if (!isMember) {
            throw new RuntimeException("User not part of this group");
        }
    }

    private GroupExpense createExpense(
            CreateGroupExpenseRequest request,
            Group group,
            User currentUser
    ) {

        GroupExpense expense = GroupExpense.builder()
                .title(request.getTitle())
                .totalAmount(request.getTotalAmount())
                .expenseDate(request.getExpenseDate())
                .paidBy(currentUser)
                .group(group)
                .splitType(request.getSplitType())
                .createdAt(LocalDateTime.now())
                .build();

        return groupExpenseRepository.save(expense);
    }

    private void handleEqualSplit(GroupExpense expense, Group group){

        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        if(members.isEmpty()){
            throw new RuntimeException("No members in group");
        }

        BigDecimal total = expense.getTotalAmount();

        BigDecimal splitAmount = total.divide(
                BigDecimal.valueOf(members.size()),
                2,
                RoundingMode.HALF_UP
        );

        for( GroupMember member : members) {
            GroupExpenseSplit split = GroupExpenseSplit.builder()
                    .expense(expense)
                    .user(member.getUser())
                    .amountOwed(splitAmount)
                    .build();

            groupExpenseSplitRepository.save(split);
        }
    }

    private void handleUnequalSplit(
            GroupExpense expense,
            Group group,
            List<SplitDetail> splits
    ) {

        if (splits == null || splits.isEmpty()) {
            throw new RuntimeException("Split details required");
        }

        List<GroupMember> members =
                groupMemberRepository.findByGroup(group);

        if (splits.size() != members.size()) {
            throw new RuntimeException("All group members must be included");
        }

        BigDecimal totalCalculated = BigDecimal.ZERO;

        for (SplitDetail detail : splits) {

            if (detail.getAmount() == null ||
                    detail.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Split amount cannot be negative");
            }

            totalCalculated = totalCalculated.add(detail.getAmount());
        }

        if (totalCalculated.compareTo(expense.getTotalAmount()) != 0) {
            throw new RuntimeException("Split amounts must equal total amount");
        }

        for (SplitDetail detail : splits) {

            User user = userRepository.findById(detail.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate split user is a group member
            if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
                throw new RuntimeException(
                        "Cannot split with non-member: " + user.getName());
            }

            GroupExpenseSplit split = GroupExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amountOwed(detail.getAmount())
                    .build();

            groupExpenseSplitRepository.save(split);
        }
    }

    private void handlePercentageSplit(
            GroupExpense expense,
            Group group,
            List<SplitDetail> splits
    ) {

        if (splits == null || splits.isEmpty()) {
            throw new RuntimeException("Split details required");
        }

        List<GroupMember> members =
                groupMemberRepository.findByGroup(group);

        if (splits.size() != members.size()) {
            throw new RuntimeException("All group members must be included");
        }

        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (SplitDetail detail : splits) {

            if (detail.getPercentage() == null ||
                    detail.getPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                    detail.getPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException(
                        "Percentage must be between 0 and 100");
            }

            totalPercentage = totalPercentage.add(detail.getPercentage());
        }

        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new RuntimeException("Percentages must sum to 100");
        }

        BigDecimal totalAmount = expense.getTotalAmount();
        BigDecimal calculatedTotal = BigDecimal.ZERO;

        List<GroupExpenseSplit> splitEntities = new ArrayList<>();

        for (SplitDetail detail : splits) {

            User user = userRepository.findById(detail.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate split user is a group member
            if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
                throw new RuntimeException(
                        "Cannot split with non-member: " + user.getName());
            }

            BigDecimal amount = totalAmount
                    .multiply(detail.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            calculatedTotal = calculatedTotal.add(amount);

            splitEntities.add(
                    GroupExpenseSplit.builder()
                            .expense(expense)
                            .user(user)
                            .amountOwed(amount)
                            .build()
            );
        }

        // Remainder correction (important)
        BigDecimal difference = totalAmount.subtract(calculatedTotal);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            splitEntities.get(0).setAmountOwed(
                    splitEntities.get(0).getAmountOwed().add(difference)
            );
        }

        groupExpenseSplitRepository.saveAll(splitEntities);
    }

    // ── Edit Expense Protection ──

    @Transactional
    public void updateExpense(UUID expenseId, UpdateGroupExpenseRequest request) {

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Group group = expense.getGroup();
        User currentUser = securityUtils.getCurrentUser();

        validateUserIsGroupMember(group, currentUser);

        // Block editing if payments exist in this group
        if (paymentRepository.existsByGroupId(group.getId())) {
            throw new RuntimeException(
                    "Cannot edit expense: payments already exist in this group. " +
                    "Editing would break settled balances.");
        }

        // Validate total amount is positive
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        // Delete old splits
        groupExpenseSplitRepository.deleteByExpense(expense);

        // Update expense fields
        expense.setTitle(request.getTitle());
        expense.setTotalAmount(request.getTotalAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setSplitType(request.getSplitType());
        groupExpenseRepository.save(expense);

        // Recreate splits
        switch (request.getSplitType()) {
            case EQUAL -> handleEqualSplit(expense, group);
            case UNEQUAL -> handleUnequalSplit(expense, group, request.getSplits());
            case PERCENTAGE -> handlePercentageSplit(expense, group, request.getSplits());
        }
    }

    // ── Delete Expense Protection ──

    @Transactional
    public void deleteExpense(UUID expenseId) {

        GroupExpense expense = groupExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        Group group = expense.getGroup();
        User currentUser = securityUtils.getCurrentUser();

        validateUserIsGroupMember(group, currentUser);

        // Block deletion if payments exist in this group
        if (paymentRepository.existsByGroupId(group.getId())) {
            throw new RuntimeException(
                    "Cannot delete expense: payments already exist in this group. " +
                    "Deleting would break settled balances.");
        }

        // Delete splits first, then the expense
        groupExpenseSplitRepository.deleteByExpense(expense);
        groupExpenseRepository.delete(expense);
    }
}
