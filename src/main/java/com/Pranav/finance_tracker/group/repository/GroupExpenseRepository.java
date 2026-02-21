package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    List<GroupExpense> findByGroup(Group group);
    List<GroupExpense> findByGroupId(UUID groupId);

    // ── Direct expense queries (group IS NULL) ──

    @Query("SELECT e FROM GroupExpense e WHERE e.group IS NULL " +
            "AND ((e.paidBy.id = :user1 AND e.otherUser.id = :user2) " +
            "OR (e.paidBy.id = :user2 AND e.otherUser.id = :user1))")
    List<GroupExpense> findDirectExpensesBetween(
            @Param("user1") UUID user1, @Param("user2") UUID user2);
}
