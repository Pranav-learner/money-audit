package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByGroupId(UUID groupId);

    List<Payment> findByGroupIdAndFromUserIdAndToUserId(UUID groupId, UUID fromUserId, UUID toUserId);

    void deleteByGroupIdAndFromUserIdAndToUserId(UUID groupId, UUID fromUserId, UUID toUserId);

    boolean existsByRequestId(String requestId);

    boolean existsByGroupId(UUID groupId);
}
