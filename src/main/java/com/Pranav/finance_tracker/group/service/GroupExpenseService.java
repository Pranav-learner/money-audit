package com.Pranav.finance_tracker.group.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
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
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
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
public class GroupExpenseService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupExpenseRepository groupExpenseRepository;
    private final GroupExpenseSplitRepository groupExpenseSplitRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;
    private final com.Pranav.finance_tracker.category.repository.CategoryRepository categoryRepository;

    @Transactional
    public GroupExpense createGroupExpense(CreateGroupExpenseRequest request ){
        User currenntUser = securityUtils.getCurrentUser();
        Group group = null;
        User otherUser = null;

        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(()-> new RuntimeException("Group not found"));
            validateUserIsGroupMember(group, currenntUser);
        } else if (request.getOtherUserId() != null) {
            otherUser = userRepository.findById(request.getOtherUserId())
                    .orElseThrow(() -> new RuntimeException("Other user not found"));
        } else {
            throw new RuntimeException("Either Group ID or Other User ID must be provided");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        GroupExpense expense = createExpense(request, group, otherUser, currenntUser);

        switch (request.getSplitType()) {
            case EQUAL -> handleEqualSplit(expense, group, otherUser, currenntUser);
            case UNEQUAL -> handleUnequalSplit(expense, group, otherUser, request.getSplits());
            case PERCENTAGE -> handlePercentageSplit(expense, group, otherUser, request.getSplits());
        }

        // Send Notification if group exists
        if (group != null) {
            List<GroupMember> members = groupMemberRepository.findByGroup(group);
            for (GroupMember member : members) {
                if (!member.getUser().getId().equals(currenntUser.getId())) {
                    String subject = "New Group Expense: " + expense.getTitle();
                    String body = String.format("Hello %s,\n\nA new expense '%s' of %.2f has been added to group '%s' by %s.",
                            member.getUser().getName(), expense.getTitle(), expense.getTotalAmount(),
                            group.getName(), currenntUser.getName());
                    emailService.sendEmail(member.getUser(), subject, body);
                }
            }
        }
        return expense;
    }

    private void validateUserIsGroupMember(Group group, User user) {
        boolean isMember = groupMemberRepository.existsByGroupAndUser(group, user);
        if (!isMember) {
            throw new RuntimeException("User not part of this group");
        }
    }

    private GroupExpense createExpense(
            CreateGroupExpenseRequest request, Group group, User otherUser, User currentUser) {

        com.Pranav.finance_tracker.category.entity.Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        GroupExpense expense = GroupExpense.builder()
                .title(request.getTitle())
                .totalAmount(request.getTotalAmount())
                .expenseDate(request.getExpenseDate())
                .paidBy(currentUser)
                .group(group)
                .otherUser(otherUser)
                .category(category)
                .splitType(request.getSplitType())
                .createdAt(LocalDateTime.now())
                .build();

        return groupExpenseRepository.save(expense);
    }

    private void handleEqualSplit(GroupExpense expense, Group group, User otherUser, User currentUser){
        List<User> participants = new ArrayList<>();
        if (group != null) {
            participants.addAll(groupMemberRepository.findByGroup(group).stream().map(GroupMember::getUser).toList());
        } else {
            participants.add(currentUser);
            participants.add(otherUser);
        }

        if(participants.isEmpty()){
            throw new RuntimeException("No participants to split with");
        }

        BigDecimal total = expense.getTotalAmount();
        BigDecimal splitAmount = total.divide(
                BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);

        for(User user : participants) {
            GroupExpenseSplit split = GroupExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amountOwed(splitAmount)
                    .build();
            groupExpenseSplitRepository.save(split);
        }
    }

    private void handleUnequalSplit(
            GroupExpense expense, Group group, User otherUser, List<SplitDetail> splits) {

        if (splits == null || splits.isEmpty()) {
            throw new RuntimeException("Split details required");
        }

        BigDecimal totalCalculated = BigDecimal.ZERO;
        for (SplitDetail detail : splits) {
            if (detail.getAmount() == null || detail.getAmount().compareTo(BigDecimal.ZERO) < 0) {
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

            if (group != null && !groupMemberRepository.existsByGroupAndUser(group, user)) {
                throw new RuntimeException("Cannot split with non-member: " + user.getName());
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
            GroupExpense expense, Group group, User otherUser, List<SplitDetail> splits) {

        if (splits == null || splits.isEmpty()) {
            throw new RuntimeException("Split details required");
        }

        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (SplitDetail detail : splits) {
            if (detail.getPercentage() == null ||
                    detail.getPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                    detail.getPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Percentage must be between 0 and 100");
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

            if (group != null && !groupMemberRepository.existsByGroupAndUser(group, user)) {
                throw new RuntimeException("Cannot split with non-member: " + user.getName());
            }

            BigDecimal amount = totalAmount
                    .multiply(detail.getPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            calculatedTotal = calculatedTotal.add(amount);

            splitEntities.add(GroupExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amountOwed(amount)
                    .build());
        }

        BigDecimal difference = totalAmount.subtract(calculatedTotal);
        if (difference.compareTo(BigDecimal.ZERO) != 0 && !splitEntities.isEmpty()) {
            splitEntities.get(0).setAmountOwed(
                    splitEntities.get(0).getAmountOwed().add(difference));
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

        if (paymentRepository.existsByGroupId(group.getId())) {
            throw new RuntimeException(
                    "Cannot edit expense: payments already exist in this group. " +
                    "Editing would break settled balances.");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        groupExpenseSplitRepository.deleteByExpense(expense);

        expense.setTitle(request.getTitle());
        expense.setTotalAmount(request.getTotalAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setSplitType(request.getSplitType());
        groupExpenseRepository.save(expense);

        switch (request.getSplitType()) {
            case EQUAL -> handleEqualSplit(expense, group, null, currentUser);
            case UNEQUAL -> handleUnequalSplit(expense, group, null, request.getSplits());
            case PERCENTAGE -> handlePercentageSplit(expense, group, null, request.getSplits());
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

        if (paymentRepository.existsByGroupId(group.getId())) {
            throw new RuntimeException(
                    "Cannot delete expense: payments already exist in this group. " +
                    "Deleting would break settled balances.");
        }

        groupExpenseSplitRepository.deleteByExpense(expense);
        groupExpenseRepository.delete(expense);
    }

    public List<java.util.Map<String, Object>> getGroupExpenses(UUID groupId) {
        return groupExpenseRepository.findByGroupId(groupId).stream()
                .map(e -> java.util.Map.of(
                        "id", (Object) e.getId(),
                        "title", (Object) e.getTitle(),
                        "amount", (Object) e.getTotalAmount(),
                        "paidBy", (Object) e.getPaidBy().getName(),
                        "date", (Object) e.getExpenseDate().toString(),
                        "splitType", (Object) e.getSplitType().name()
                ))
                .toList();
    }
}
