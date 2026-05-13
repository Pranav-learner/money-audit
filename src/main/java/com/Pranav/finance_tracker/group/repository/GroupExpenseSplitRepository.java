package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GroupExpenseSplitRepository extends JpaRepository<GroupExpenseSplit, UUID> {

        List<GroupExpenseSplit> findByUser(User user);

        @Query("SELECT s FROM GroupExpenseSplit s WHERE s.expense.group.id = :groupId")
        List<GroupExpenseSplit> findByExpenseGroupId(@Param("groupId") UUID groupId);

        List<GroupExpenseSplit> findByExpense(GroupExpense expense);

        void deleteByExpense(GroupExpense expense);

        // ── Balance queries: ONLY count unsettled splits ──

        @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
                        "WHERE s.user.id = :fromUser AND s.expense.paidBy.id = :toUser " +
                        "AND s.isSettled = false")
        BigDecimal sumDirectDebt(@Param("fromUser") UUID fromUser, @Param("toUser") UUID toUser);

        @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
                        "WHERE s.user.id = :userId AND s.expense.paidBy.id != :userId " +
                        "AND s.isSettled = false")
        BigDecimal sumTotalOwedByUser(@Param("userId") UUID userId);

        @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
                        "WHERE s.expense.paidBy.id = :userId AND s.user.id != :userId " +
                        "AND s.isSettled = false")
        BigDecimal sumTotalOwedToUser(@Param("userId") UUID userId);

        // ── Ledger: mark settled instead of deleting ──

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE GroupExpenseSplit s SET s.isSettled = true " +
                        "WHERE s.isSettled = false AND s.expense.group IS NULL " +
                        "AND ((s.expense.paidBy.id = :u1 AND s.expense.otherUser.id = :u2) " +
                        "OR (s.expense.paidBy.id = :u2 AND s.expense.otherUser.id = :u1))")
        void markDirectSplitsSettled(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
