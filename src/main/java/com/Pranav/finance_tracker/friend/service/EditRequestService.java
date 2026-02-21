package com.Pranav.finance_tracker.friend.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.friend.dto.CreateEditRequestDTO;
import com.Pranav.finance_tracker.friend.entity.EditRequest;
import com.Pranav.finance_tracker.friend.enums.EditRequestStatus;
import com.Pranav.finance_tracker.friend.repository.EditRequestRepository;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.group.repository.GroupExpenseRepository;
import com.Pranav.finance_tracker.group.repository.GroupExpenseSplitRepository;
import com.Pranav.finance_tracker.payment.repository.PaymentRepository;
import com.Pranav.finance_tracker.user.entity.User;
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
public class EditRequestService {

    private final EditRequestRepository editRequestRepository;
    private final GroupExpenseRepository expenseRepository;
    private final GroupExpenseSplitRepository splitRepository;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;

    // ── Create edit request ──

    @Transactional
    public String createEditRequest(UUID expenseId, CreateEditRequestDTO dto) {

        User currentUser = securityUtils.getCurrentUser();

        GroupExpense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Must be a direct expense (group = null)
        if (expense.getGroup() != null) {
            throw new RuntimeException("Edit requests are only for direct expenses");
        }

        // Must be a participant but NOT the creator
        UUID payerId = expense.getPaidBy().getId();
        UUID otherId = expense.getOtherUser().getId();

        if (!currentUser.getId().equals(payerId) && !currentUser.getId().equals(otherId)) {
            throw new RuntimeException("You are not a participant of this expense");
        }

        if (currentUser.getId().equals(payerId)) {
            throw new RuntimeException("Expense creator cannot request edits — edit directly instead");
        }

        // Check no direct payments exist between these two users
        if (paymentRepository.existsDirectPaymentBetween(payerId, otherId)) {
            throw new RuntimeException(
                    "Cannot request edit: payments already exist. " +
                    "Editing would break settled balances.");
        }

        // Check no pending edit request already exists for this expense
        if (editRequestRepository.existsByExpenseAndStatus(expense, EditRequestStatus.PENDING)) {
            throw new RuntimeException("A pending edit request already exists for this expense");
        }

        // Validate new amount
        if (dto.getNewAmount() == null || dto.getNewAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("New amount must be positive");
        }

        EditRequest editRequest = EditRequest.builder()
                .expense(expense)
                .requestedBy(currentUser)
                .requestedTo(expense.getPaidBy())
                .newAmount(dto.getNewAmount())
                .note(dto.getNote())
                .status(EditRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        editRequestRepository.save(editRequest);
        return "Edit request submitted";
    }

    // ── Approve edit request ──

    @Transactional
    public String approve(UUID editRequestId) {

        User currentUser = securityUtils.getCurrentUser();

        EditRequest editRequest = editRequestRepository.findById(editRequestId)
                .orElseThrow(() -> new RuntimeException("Edit request not found"));

        if (!editRequest.getRequestedTo().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the expense creator can approve edit requests");
        }

        if (editRequest.getStatus() != EditRequestStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending");
        }

        GroupExpense expense = editRequest.getExpense();

        // Re-check: no direct payments must exist
        if (paymentRepository.existsDirectPaymentBetween(
                expense.getPaidBy().getId(), expense.getOtherUser().getId())) {
            throw new RuntimeException("Cannot approve: payments now exist between users");
        }

        // Update expense amount
        expense.setTotalAmount(editRequest.getNewAmount());
        expenseRepository.save(expense);

        // Delete old splits and recalculate
        splitRepository.deleteByExpense(expense);
        recalculateSplits(expense);

        // Mark approved
        editRequest.setStatus(EditRequestStatus.APPROVED);
        editRequest.setResolvedAt(LocalDateTime.now());
        editRequestRepository.save(editRequest);

        return "Edit request approved — expense updated to " + editRequest.getNewAmount();
    }

    // ── Reject edit request ──

    @Transactional
    public String reject(UUID editRequestId) {

        User currentUser = securityUtils.getCurrentUser();

        EditRequest editRequest = editRequestRepository.findById(editRequestId)
                .orElseThrow(() -> new RuntimeException("Edit request not found"));

        if (!editRequest.getRequestedTo().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the expense creator can reject edit requests");
        }

        if (editRequest.getStatus() != EditRequestStatus.PENDING) {
            throw new RuntimeException("This request is no longer pending");
        }

        editRequest.setStatus(EditRequestStatus.REJECTED);
        editRequest.setResolvedAt(LocalDateTime.now());
        editRequestRepository.save(editRequest);

        return "Edit request rejected";
    }

    // ── List pending requests for current user ──

    public List<EditRequest> getPendingRequests() {
        User currentUser = securityUtils.getCurrentUser();
        return editRequestRepository.findByRequestedToAndStatus(
                currentUser, EditRequestStatus.PENDING);
    }

    // ── Recalculate splits after edit ──

    private void recalculateSplits(GroupExpense expense) {

        User payer = expense.getPaidBy();
        User other = expense.getOtherUser();
        BigDecimal total = expense.getTotalAmount();

        BigDecimal half = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal remainder = total.subtract(half.multiply(BigDecimal.valueOf(2)));

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(payer).amountOwed(half).build());

        splitRepository.save(GroupExpenseSplit.builder()
                .expense(expense).user(other).amountOwed(half.add(remainder)).build());
    }
}
