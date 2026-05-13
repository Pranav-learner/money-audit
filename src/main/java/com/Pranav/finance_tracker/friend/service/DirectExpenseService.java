package com.Pranav.finance_tracker.friend.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.friend.dto.CreateDirectExpenseRequest;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DirectExpenseService {

    private final GroupExpenseRepository expenseRepository;
    private final GroupExpenseSplitRepository splitRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailService emailService;

    @Transactional
    public String createExpense(CreateDirectExpenseRequest request) {

        User currentUser = securityUtils.getCurrentUser();

        User otherUser;
        if (request.getFriendId() != null) {
            otherUser = userRepository.findById(request.getFriendId())
                    .orElseThrow(() -> new RuntimeException("No user found with id: " + request.getFriendId()));
        } else {
            otherUser = userRepository.findByPhone(request.getOtherUserPhone())
                    .orElseThrow(() -> new RuntimeException(
                            "No user found with phone: " + request.getOtherUserPhone()));
        }

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new RuntimeException("Cannot create expense with yourself");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        User payer = currentUser;
        User nonPayer = otherUser;

        if (request.getPaidByUserId() != null && request.getPaidByUserId().equals(otherUser.getId())) {
            payer = otherUser;
            nonPayer = currentUser;
        }

        // Create expense with group = null (direct)
        GroupExpense expense = GroupExpense.builder()
                .title(request.getTitle())
                .totalAmount(request.getTotalAmount())
                .expenseDate(request.getExpenseDate())
                .paidBy(payer)
                .otherUser(nonPayer)
                .splitType(request.getSplitType())
                .group(null) // key: null = direct expense
                .createdAt(LocalDateTime.now())
                .build();

        expenseRepository.save(expense);

        switch (request.getSplitType()) {
            case EQUAL -> handleEqualSplit(expense, currentUser, otherUser);
            case UNEQUAL -> handleUnequalSplit(expense, currentUser, otherUser, request);
            case PERCENTAGE -> handlePercentageSplit(expense, currentUser, otherUser, request);
        }

        // Send Notification to the other participant (whoever it is)
        User notificationRecipient = (payer.getId().equals(currentUser.getId())) ? otherUser : currentUser;
        String subject = "New Direct Expense: " + expense.getTitle();
        String body = String.format("Hello %s,\n\n%s has added a direct expense: '%s' of %.2f.",
                notificationRecipient.getName(), payer.getName(), expense.getTitle(), expense.getTotalAmount());
        emailService.sendEmail(notificationRecipient, subject, body);

        return "Direct expense created successfully";
    }

    public List<GroupExpense> getExpenseHistory(String otherUserPhone) {
        User currentUser = securityUtils.getCurrentUser();
        User otherUser = userRepository.findByPhone(otherUserPhone)
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + otherUserPhone));
        return expenseRepository.findDirectExpensesBetween(currentUser.getId(), otherUser.getId());
    }

    public List<GroupExpense> getExpenseHistoryByUserId(UUID otherUserId) {
        User currentUser = securityUtils.getCurrentUser();
        return expenseRepository.findDirectExpensesBetween(currentUser.getId(), otherUserId);
    }

    public List<GroupExpense> getAllExpensesByUser() {
        User currentUser = securityUtils.getCurrentUser();
        return expenseRepository.findAllDirectExpensesByUser(currentUser.getId());
    }

    public java.util.Map<String, Long> getBorrowRate(UUID friendId) {
        User currentUser = securityUtils.getCurrentUser();
        
        long lentCount = expenseRepository.countDirectExpensesAsPayer(currentUser.getId(), friendId);
        long borrowCount = expenseRepository.countDirectExpensesAsPayer(friendId, currentUser.getId());
        
        return java.util.Map.of("borrowCount", borrowCount, "lendCount", lentCount);
    }

    // ── Split handlers ──

    private void handleEqualSplit(GroupExpense expense, User payer, User other) {
        BigDecimal total = expense.getTotalAmount();
        BigDecimal half = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal remainder = total.subtract(half.multiply(BigDecimal.valueOf(2)));

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(payer).amountOwed(half).build());
        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(other).amountOwed(half.add(remainder)).build());
    }

    private void handleUnequalSplit(GroupExpense expense, User currentUser, User otherUser,
                                    CreateDirectExpenseRequest request) {
        BigDecimal myShare = request.getMyShare() != null ? request.getMyShare() : BigDecimal.ZERO;
        BigDecimal otherShare = request.getOtherShare() != null ? request.getOtherShare() : BigDecimal.ZERO;

        if (myShare.add(otherShare).compareTo(expense.getTotalAmount()) != 0) {
            throw new RuntimeException("Shares must sum to total amount. Sum: " + myShare.add(otherShare) + ", Total: " + expense.getTotalAmount());
        }

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(currentUser).amountOwed(myShare).build());
        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(otherUser).amountOwed(otherShare).build());
    }

    private void handlePercentageSplit(GroupExpense expense, User currentUser, User otherUser,
                                       CreateDirectExpenseRequest request) {
        BigDecimal myPct = request.getMyPercentage() != null ? request.getMyPercentage() : BigDecimal.ZERO;
        BigDecimal otherPct = request.getOtherPercentage() != null ? request.getOtherPercentage() : BigDecimal.ZERO;

        if (myPct.add(otherPct).compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new RuntimeException("Percentages must sum to 100");
        }

        BigDecimal total = expense.getTotalAmount();
        BigDecimal myAmount = total.multiply(myPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal otherAmount = total.subtract(myAmount); // rounding correction

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(currentUser).amountOwed(myAmount).build());
        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(otherUser).amountOwed(otherAmount).build());
    }
}
