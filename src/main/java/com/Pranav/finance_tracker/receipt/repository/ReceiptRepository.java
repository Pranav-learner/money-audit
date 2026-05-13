package com.Pranav.finance_tracker.receipt.repository;

import com.Pranav.finance_tracker.receipt.entity.Receipt;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByUserOrderByCreatedAtDesc(User user);
    Optional<Receipt> findByIdAndUser(UUID id, User user);
    List<Receipt> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
    Optional<Receipt> findByLinkedGroupExpenseId(UUID groupExpenseId);
}
