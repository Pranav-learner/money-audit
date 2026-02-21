package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.group.entity.GroupExpenseSplit;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GroupExpenseSplitRepository extends JpaRepository<GroupExpenseSplit, UUID> {

    List<GroupExpenseSplit> findByGroupExpense(GroupExpense expense);

    List<GroupExpenseSplit> findByUser(User user);

    @Query("SELECT s FROM GroupExpenseSplit s WHERE s.expense.group.id = :groupId")
    List<GroupExpenseSplit> findByExpenseGroupId(@Param("groupId") UUID groupId);

    List<GroupExpenseSplit> findByExpense(GroupExpense expense);

    void deleteByExpense(GroupExpense expense);

    // ── SQL aggregation for direct balance — what fromUser owes toUser ──

    @Query("SELECT COALESCE(SUM(s.amountOwed), 0) FROM GroupExpenseSplit s " +
            "WHERE s.expense.group IS NULL " +
            "AND s.user.id = :fromUser AND s.expense.paidBy.id = :toUser")
    BigDecimal sumDirectDebt(@Param("fromUser") UUID fromUser, @Param("toUser") UUID toUser);
}
