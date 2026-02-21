package com.Pranav.finance_tracker.payment.repository;

import com.Pranav.finance_tracker.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // ── Group payment queries ──

    List<Payment> findByGroupId(UUID groupId);

    List<Payment> findByGroupIdAndFromUserIdAndToUserId(UUID groupId, UUID fromUserId, UUID toUserId);

    void deleteByGroupIdAndFromUserIdAndToUserId(UUID groupId, UUID fromUserId, UUID toUserId);

    boolean existsByRequestId(String requestId);

    boolean existsByGroupId(UUID groupId);

    // ── Direct payment queries (group IS NULL) ──

    @Query("SELECT p FROM Payment p WHERE p.group IS NULL " +
            "AND ((p.fromUser.id = :user1 AND p.toUser.id = :user2) " +
            "OR (p.fromUser.id = :user2 AND p.toUser.id = :user1))")
    List<Payment> findDirectPaymentsBetween(
            @Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("SELECT p FROM Payment p WHERE p.group IS NULL " +
            "AND p.fromUser.id = :fromId AND p.toUser.id = :toId")
    List<Payment> findDirectPaymentsFromTo(
            @Param("fromId") UUID fromId, @Param("toId") UUID toId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p " +
            "WHERE p.group IS NULL " +
            "AND ((p.fromUser.id = :user1 AND p.toUser.id = :user2) " +
            "OR (p.fromUser.id = :user2 AND p.toUser.id = :user1))")
    boolean existsDirectPaymentBetween(
            @Param("user1") UUID user1, @Param("user2") UUID user2);

    // SQL aggregation for direct payments
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.group IS NULL AND p.fromUser.id = :fromId AND p.toUser.id = :toId")
    BigDecimal sumDirectPaymentsFromTo(
            @Param("fromId") UUID fromId, @Param("toId") UUID toId);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.group IS NULL " +
            "AND p.fromUser.id = :fromId AND p.toUser.id = :toId")
    void deleteDirectPaymentsFromTo(
            @Param("fromId") UUID fromId, @Param("toId") UUID toId);
}
