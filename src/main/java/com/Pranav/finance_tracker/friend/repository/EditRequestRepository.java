package com.Pranav.finance_tracker.friend.repository;

import com.Pranav.finance_tracker.friend.entity.EditRequest;
import com.Pranav.finance_tracker.friend.enums.EditRequestStatus;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EditRequestRepository extends JpaRepository<EditRequest, UUID> {

    List<EditRequest> findByRequestedToAndStatus(User requestedTo, EditRequestStatus status);

    boolean existsByExpenseAndStatus(GroupExpense expense, EditRequestStatus status);
}
