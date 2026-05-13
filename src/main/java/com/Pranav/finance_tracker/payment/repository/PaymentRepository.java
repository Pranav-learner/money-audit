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

        // ── Active direct payment queries (isSettled = false) ──

        @Query("SELECT p FROM Payment p WHERE p.group IS NULL AND p.isSettled = false " +
                        "AND ((p.fromUser.id = :user1 AND p.toUser.id = :user2) " +
                        "OR (p.fromUser.id = :user2 AND p.toUser.id = :user1))")
        List<Payment> findDirectPaymentsBetween(
                        @Param("user1") UUID user1, @Param("user2") UUID user2);

        @Query("SELECT p FROM Payment p WHERE p.group IS NULL AND p.isSettled = false " +
                        "AND (p.fromUser.id = :userId OR p.toUser.id = :userId) " +
                        "ORDER BY p.createdAt DESC")
        List<Payment> findAllDirectPaymentsByUser(@Param("userId") UUID userId);

        @Query("SELECT p FROM Payment p WHERE p.group IS NULL AND p.isSettled = false " +
                        "AND p.fromUser.id = :fromId AND p.toUser.id = :toId")
        List<Payment> findDirectPaymentsFromTo(
                        @Param("fromId") UUID fromId, @Param("toId") UUID toId);

        @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p " +
                        "WHERE p.group IS NULL AND p.isSettled = false " +
                        "AND ((p.fromUser.id = :user1 AND p.toUser.id = :user2) " +
                        "OR (p.fromUser.id = :user2 AND p.toUser.id = :user1))")
        boolean existsDirectPaymentBetween(
                        @Param("user1") UUID user1, @Param("user2") UUID user2);

        // ── Balance aggregation: ONLY unsettled payments ──

        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
                        "WHERE p.group IS NULL AND p.isSettled = false " +
                        "AND p.fromUser.id = :fromId AND p.toUser.id = :toId")
        BigDecimal sumDirectPaymentsFromTo(
                        @Param("fromId") UUID fromId, @Param("toId") UUID toId);

        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.fromUser.id = :userId")
        BigDecimal sumTotalPaymentsSent(@Param("userId") UUID userId);

        @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.toUser.id = :userId")
        BigDecimal sumTotalPaymentsReceived(@Param("userId") UUID userId);

        // ── Ledger: mark settled instead of deleting ──

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE Payment p SET p.isSettled = true, p.settledAt = CURRENT_TIMESTAMP " +
                        "WHERE p.isSettled = false AND p.group IS NULL " +
                        "AND ((p.fromUser.id = :u1 AND p.toUser.id = :u2) " +
                        "OR (p.fromUser.id = :u2 AND p.toUser.id = :u1))")
        void markDirectPaymentsSettled(@Param("u1") UUID u1, @Param("u2") UUID u2);

        @Query(value = "SELECT * FROM payments p WHERE p.group_id IS NULL AND p.is_settled = true " +
                        "AND ((p.from_user = :user1 AND p.to_user = :user2) " +
                        "OR (p.from_user = :user2 AND p.to_user = :user1)) " +
                        "ORDER BY p.settled_at DESC LIMIT 1", nativeQuery = true)
        Payment findLastSettlementBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);
}
