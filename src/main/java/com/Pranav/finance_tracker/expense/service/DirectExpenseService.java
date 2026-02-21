package com.Pranav.finance_tracker.expense.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.service.EmailService;
import com.Pranav.finance_tracker.expense.dto.CreateDirectExpenseRequest;
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

        User otherUser = userRepository.findByPhone(request.getOtherUserPhone())
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + request.getOtherUserPhone()));

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new RuntimeException("Cannot create expense with yourself");
        }

        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Total amount must be positive");
        }

        // Create expense with group = null (direct)
        GroupExpense expense = GroupExpense.builder()
                .title(request.getTitle())
                .totalAmount(request.getTotalAmount())
                .expenseDate(request.getExpenseDate())
                .paidBy(currentUser)
                .otherUser(otherUser)
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

        // Send Notification
        String subject = "New Direct Expense: " + expense.getTitle();
        String body = String.format("Hello %s,\n\n%s has added a direct expense: '%s' of %.2f.",
                otherUser.getName(), currentUser.getName(), expense.getTitle(), expense.getTotalAmount());
        emailService.sendEmail(otherUser, subject, body);

        return "Direct expense created successfully";
    }

    public List<GroupExpense> getExpenseHistory(String otherUserPhone) {
        User currentUser = securityUtils.getCurrentUser();
        User otherUser = userRepository.findByPhone(otherUserPhone)
                .orElseThrow(() -> new RuntimeException(
                        "No user found with phone: " + otherUserPhone));
        return expenseRepository.findDirectExpensesBetween(currentUser.getId(), otherUser.getId());
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

    private void handleUnequalSplit(GroupExpense expense, User payer, User other,
                                    CreateDirectExpenseRequest request) {
        if (request.getMyShare().add(request.getOtherShare())
                .compareTo(expense.getTotalAmount()) != 0) {
            throw new RuntimeException("Shares must sum to total amount");
        }

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(payer).amountOwed(request.getMyShare()).build());
        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(other).amountOwed(request.getOtherShare()).build());
    }

    private void handlePercentageSplit(GroupExpense expense, User payer, User other,
                                       CreateDirectExpenseRequest request) {
        if (request.getMyPercentage().add(request.getOtherPercentage())
                .compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new RuntimeException("Percentages must sum to 100");
        }

        BigDecimal total = expense.getTotalAmount();
        BigDecimal myAmount = total.multiply(request.getMyPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal otherAmount = total.subtract(myAmount); // rounding correction

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(payer).amountOwed(myAmount).build());
        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(other).amountOwed(otherAmount).build());
    }
}
