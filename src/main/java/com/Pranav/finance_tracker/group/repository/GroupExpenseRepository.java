package com.Pranav.finance_tracker.group.repository;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroupExpenseRepository extends JpaRepository<GroupExpense, UUID> {

    List<GroupExpense> findByGroup(Group group);
    List<GroupExpense> findByGroupId(UUID groupId);
}
