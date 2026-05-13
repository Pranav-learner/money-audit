package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

        List<GroupExpense> findByGroup(Group group);

        List<GroupExpense> findByGroupId(UUID groupId);

        // ── Active direct expense queries (isSettled = false) ──

        @Query("SELECT e FROM GroupExpense e WHERE e.group IS NULL AND e.isSettled = false " +
                        "AND ((e.paidBy.id = :user1 AND e.otherUser.id = :user2) " +
                        "OR (e.paidBy.id = :user2 AND e.otherUser.id = :user1))")
        List<GroupExpense> findDirectExpensesBetween(
                        @Param("user1") UUID user1, @Param("user2") UUID user2);

        @Query("SELECT e FROM GroupExpense e WHERE e.group IS NULL AND e.isSettled = false " +
                        "AND (e.paidBy.id = :userId OR e.otherUser.id = :userId) " +
                        "ORDER BY e.createdAt DESC")
        List<GroupExpense> findAllDirectExpensesByUser(@Param("userId") UUID userId);

        // ── Analytics (still uses all records, settled or not) ──

        @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM GroupExpense e " +
                        "WHERE e.paidBy.id = :userId AND e.group IS NULL")
        BigDecimal sumTotalDirectExpenseAsPayer(@Param("userId") UUID userId);

        @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM GroupExpense e " +
                        "WHERE e.paidBy.id = :userId AND e.group IS NOT NULL")
        BigDecimal sumTotalGroupExpenseAsPayer(@Param("userId") UUID userId);

        @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
                        "WHERE s.user.id = :userId AND s.expense.group IS NULL " +
                        "AND s.expense.paidBy.id != :userId")
        BigDecimal sumTotalDirectExpenseAsParticipant(@Param("userId") UUID userId);

        @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
                        "WHERE s.user.id = :userId AND s.expense.group IS NOT NULL " +
                        "AND s.expense.paidBy.id != :userId")
        BigDecimal sumTotalGroupExpenseAsParticipant(@Param("userId") UUID userId);

        @Query("SELECT COUNT(e) FROM GroupExpense e WHERE e.group IS NULL " +
                        "AND e.paidBy.id = :payerId AND e.otherUser.id = :otherId")
        long countDirectExpensesAsPayer(@Param("payerId") UUID payerId, @Param("otherId") UUID otherId);

        // ── Ledger: mark settled instead of deleting ──

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE GroupExpense e SET e.isSettled = true, e.settledAt = CURRENT_TIMESTAMP " +
                        "WHERE e.isSettled = false AND e.group IS NULL " +
                        "AND ((e.paidBy.id = :u1 AND e.otherUser.id = :u2) " +
                        "OR (e.paidBy.id = :u2 AND e.otherUser.id = :u1))")
        void markDirectExpensesSettled(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
